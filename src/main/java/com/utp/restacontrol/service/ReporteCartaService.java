package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.reporte.carta.CartaItemResponse;
import com.utp.restacontrol.dto.reporte.carta.CartaReporteResponse;
import com.utp.restacontrol.repository.ReporteCartaRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ReporteCartaService {

    private final ReporteCartaRepository repository;

    public ReporteCartaService(
            ReporteCartaRepository repository) {

        this.repository = repository;
    }

    public CartaReporteResponse listar(
            String tipo,
            UUID idCategoria,
            String estado,
            String disponibilidad,
            String search,
            int page,
            int size) {

        int safePage = Math.max(page, 1);

        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        validarTipo(tipo);
        validarEstado(estado);
        validarDisponibilidad(disponibilidad);

        var data = repository.listar(
                tipo,
                idCategoria,
                estado,
                disponibilidad,
                search,
                safePage,
                safeSize
        );

        long total = repository.contar(
                tipo,
                idCategoria,
                estado,
                disponibilidad,
                search
        );

        var summary = repository.obtenerResumen(
                tipo,
                idCategoria,
                estado,
                disponibilidad,
                search
        );

        return new CartaReporteResponse(
                true,
                "Reporte de carta obtenido correctamente",
                data,
                total,
                safePage,
                safeSize,
                summary
        );
    }

    public CartaItemResponse obtenerDetalle(
            String tipo,
            UUID id) {

        validarTipoObligatorio(tipo);

        return repository.obtenerDetalle(tipo, id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "El elemento solicitado no existe."
                        )
                );
    }

    public byte[] exportarExcel(
            String tipo,
            UUID idCategoria,
            String estado,
            String disponibilidad,
            String search) {

        validarTipo(tipo);
        validarEstado(estado);
        validarDisponibilidad(disponibilidad);

        List<CartaItemResponse> registros =
                repository.listar(
                        tipo,
                        idCategoria,
                        estado,
                        disponibilidad,
                        search,
                        1,
                        10000
                );

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet(
                    "Reporte de Carta"
            );

            crearTituloExcel(workbook, sheet);
            crearEncabezadoExcel(workbook, sheet);

            CellStyle monedaStyle =
                    crearEstiloMoneda(workbook);

            int filaActual = 2;

            for (CartaItemResponse item : registros) {
                Row fila = sheet.createRow(filaActual++);

                fila.createCell(0).setCellValue(
                        texto(item.tipo()).toUpperCase()
                );

                fila.createCell(1).setCellValue(
                        texto(item.codigo())
                );

                fila.createCell(2).setCellValue(
                        texto(item.nombre())
                );

                fila.createCell(3).setCellValue(
                        texto(item.categoriaNombre())
                );

                Cell precioCell = fila.createCell(4);
                precioCell.setCellValue(
                        decimal(item.precio())
                );
                precioCell.setCellStyle(monedaStyle);

                Cell stockCell = fila.createCell(5);

                if (item.stock() == null) {
                    stockCell.setCellValue("No aplica");
                } else {
                    stockCell.setCellValue(item.stock());
                }

                fila.createCell(6).setCellValue(
                        texto(item.unidadMedida())
                );

                fila.createCell(7).setCellValue(
                        Boolean.TRUE.equals(item.disponible())
                                ? "Disponible"
                                : "No disponible"
                );

                fila.createCell(8).setCellValue(
                        Boolean.TRUE.equals(item.activo())
                                ? "Activo"
                                : "Inactivo"
                );

                fila.createCell(9).setCellValue(
                        texto(item.descripcion())
                );
            }

            aplicarAnchosExcel(sheet);

            sheet.createFreezePane(0, 2);

            if (!registros.isEmpty()) {
                sheet.setAutoFilter(
                        new CellRangeAddress(
                                1,
                                filaActual - 1,
                                0,
                                9
                        )
                );
            }

            workbook.write(output);

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo generar el reporte de carta en Excel.",
                    exception
            );
        }
    }

    private void crearTituloExcel(
            Workbook workbook,
            Sheet sheet) {

        Row fila = sheet.createRow(0);
        Cell celda = fila.createCell(0);

        celda.setCellValue(
                "RestaControl - Reporte de Carta"
        );

        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();

        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 16);

        estilo.setFont(fuente);
        estilo.setAlignment(
                HorizontalAlignment.CENTER
        );

        celda.setCellStyle(estilo);

        sheet.addMergedRegion(
                new CellRangeAddress(
                        0,
                        0,
                        0,
                        9
                )
        );
    }

    private void crearEncabezadoExcel(
            Workbook workbook,
            Sheet sheet) {

        String[] columnas = {
                "Tipo",
                "Código",
                "Nombre",
                "Categoría",
                "Precio",
                "Stock",
                "Unidad",
                "Disponibilidad",
                "Estado",
                "Descripción"
        };

        Row fila = sheet.createRow(1);

        CellStyle estilo = workbook.createCellStyle();
        Font fuente = workbook.createFont();

        fuente.setBold(true);
        estilo.setFont(fuente);
        estilo.setAlignment(
                HorizontalAlignment.CENTER
        );

        for (int i = 0; i < columnas.length; i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(columnas[i]);
            celda.setCellStyle(estilo);
        }
    }

    private CellStyle crearEstiloMoneda(
            Workbook workbook) {

        CellStyle estilo = workbook.createCellStyle();
        DataFormat formato = workbook.createDataFormat();

        estilo.setDataFormat(
                formato.getFormat(
                        "\"S/\" #,##0.00"
                )
        );

        return estilo;
    }


    private void aplicarAnchosExcel(
            Sheet sheet) {

        sheet.setColumnWidth(0, 14 * 256);
        sheet.setColumnWidth(1, 15 * 256);
        sheet.setColumnWidth(2, 28 * 256);
        sheet.setColumnWidth(3, 22 * 256);
        sheet.setColumnWidth(4, 14 * 256);
        sheet.setColumnWidth(5, 12 * 256);
        sheet.setColumnWidth(6, 16 * 256);
        sheet.setColumnWidth(7, 18 * 256);
        sheet.setColumnWidth(8, 14 * 256);
        sheet.setColumnWidth(9, 45 * 256);
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private double decimal(BigDecimal valor) {
        return valor == null
                ? 0
                : valor.doubleValue();
    }


    private void validarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return;
        }

        validarTipoObligatorio(tipo);
    }

    private void validarTipoObligatorio(String tipo) {
        String value = tipo == null
                ? ""
                : tipo.trim().toLowerCase();

        if (
            !"plato".equals(value) &&
            !"producto".equals(value)
        ) {
            throw new IllegalArgumentException(
                    "El tipo debe ser plato o producto."
            );
        }
    }

    private void validarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            return;
        }

        String value = estado.trim().toLowerCase();

        if (
            !"activo".equals(value) &&
            !"inactivo".equals(value)
        ) {
            throw new IllegalArgumentException(
                    "El estado debe ser activo o inactivo."
            );
        }
    }

    private void validarDisponibilidad(
            String disponibilidad) {

        if (
            disponibilidad == null ||
            disponibilidad.isBlank()
        ) {
            return;
        }

        String value = disponibilidad
                .trim()
                .toLowerCase();

        if (
            !"disponible".equals(value) &&
            !"no_disponible".equals(value)
        ) {
            throw new IllegalArgumentException(
                    "La disponibilidad debe ser disponible o no_disponible."
            );
        }
    }
}
package com.utp.restacontrol.service;

import com.utp.restacontrol.dto.reporte.ConsumoMesaDetalleResponse;
import com.utp.restacontrol.dto.reporte.ConsumoMesaReporteResponse;
import com.utp.restacontrol.repository.ReporteConsumosMesaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;
import com.utp.restacontrol.dto.reporte.ConsumoMesaResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ReporteConsumosMesaService {

    private final ReporteConsumosMesaRepository repository;

    public ReporteConsumosMesaService(
            ReporteConsumosMesaRepository repository) {
        this.repository = repository;
    }

    public ConsumoMesaReporteResponse listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            UUID idMesa,
            String estado,
            String search,
            int page,
            int size) {

        int safePage = Math.max(page, 1);
        int safeSize = Math.min(
                Math.max(size, 1),
                100
        );

        if (
            fechaDesde != null
            && fechaHasta != null
            && fechaDesde.isAfter(fechaHasta)
        ) {
            throw new IllegalArgumentException(
                    "La fecha desde no puede ser posterior a la fecha hasta."
            );
        }

        var data = repository.listar(
                fechaDesde,
                fechaHasta,
                idMesa,
                estado,
                search,
                safePage,
                safeSize
        );

        long total = repository.contar(
                fechaDesde,
                fechaHasta,
                idMesa,
                estado,
                search
        );

        var summary = repository.obtenerResumen(
                fechaDesde,
                fechaHasta,
                idMesa,
                estado,
                search
        );

        return new ConsumoMesaReporteResponse(
                true,
                "Reporte obtenido correctamente",
                data,
                total,
                safePage,
                safeSize,
                summary
        );
    }

    public ConsumoMesaDetalleResponse obtenerDetalle(
            UUID idAtencion) {

        return repository.obtenerDetalle(idAtencion)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "La atención solicitada no existe."
                        )
                );
    }


    public byte[] exportarExcel(
                LocalDate fechaDesde,
                LocalDate fechaHasta,
                UUID idMesa,
                String estado,
                String search) {

        List<ConsumoMesaResponse> registros =
                repository.listar(
                        fechaDesde,
                        fechaHasta,
                        idMesa,
                        estado,
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
                        "Consumos por mesa"
                );

                crearEncabezadoExcel(workbook, sheet);

                int rowIndex = 1;

                for (ConsumoMesaResponse registro : registros) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(
                        registro.fechaCierre() != null
                                ? registro.fechaCierre().toString()
                                : registro.fechaApertura() != null
                                        ? registro.fechaApertura().toString()
                                        : ""
                );

                row.createCell(1).setCellValue(
                        valorTexto(registro.codigoAtencion())
                );

                row.createCell(2).setCellValue(
                        valorTexto(registro.mesaCodigo())
                );

                row.createCell(3).setCellValue(
                        valorTexto(registro.clienteNombre())
                );

                row.createCell(4).setCellValue(
                        valorTexto(registro.mozoNombre())
                );

                row.createCell(5).setCellValue(
                        registro.cantidadItems() == null
                                ? 0
                                : registro.cantidadItems()
                );

                row.createCell(6).setCellValue(
                        valorDecimal(registro.subtotal())
                );

                row.createCell(7).setCellValue(
                        valorDecimal(registro.propina())
                );

                row.createCell(8).setCellValue(
                        valorDecimal(registro.total())
                );

                row.createCell(9).setCellValue(
                        valorTexto(registro.estadoAtencion())
                );

                row.createCell(10).setCellValue(
                        valorTexto(registro.estadoPago())
                );
                }

                for (int column = 0; column <= 10; column++) {
                sheet.autoSizeColumn(column);
                }

                workbook.write(output);

                return output.toByteArray();

        } catch (IOException exception) {
                throw new IllegalStateException(
                        "No se pudo generar el archivo Excel.",
                        exception
                );
        }
        }

        private void crearEncabezadoExcel(
                Workbook workbook,
                Sheet sheet) {

        Row header = sheet.createRow(0);

        String[] columns = {
                "Fecha",
                "Atención",
                "Mesa",
                "Cliente",
                "Mozo",
                "Ítems",
                "Subtotal",
                "Propina",
                "Total",
                "Estado atención",
                "Estado pago"
        };

        CellStyle headerStyle =
                workbook.createCellStyle();

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);

        headerStyle.setFont(headerFont);

        for (int index = 0; index < columns.length; index++) {
                Cell cell = header.createCell(index);
                cell.setCellValue(columns[index]);
                cell.setCellStyle(headerStyle);
        }
        }

        private String valorTexto(String value) {
        return value == null ? "" : value;
        }

        private double valorDecimal(BigDecimal value) {
        return value == null
                ? 0
                : value.doubleValue();
        }

}
package com.academia.batch.processor;

import com.academia.batch.model.Empleado;
import com.academia.batch.model.EmpleadoReporte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

// Step 2 - Processor: convierte Empleado a EmpleadoReporte calculando el salario total
// Nota: el tipo de entrada (Empleado) es diferente al tipo de salida (EmpleadoReporte)
public class ReporteProcessor implements ItemProcessor<Empleado, EmpleadoReporte> {

    private static final Logger log = LoggerFactory.getLogger(ReporteProcessor.class);

    @Override
    public EmpleadoReporte process(Empleado empleado) {
        EmpleadoReporte reporte = new EmpleadoReporte();
        reporte.setNombre(empleado.getNombre());
        reporte.setDepartamento(empleado.getDepartamento());
        reporte.setSalario(empleado.getSalario());
        reporte.setBono(empleado.getBono());
        reporte.setSalarioTotal(empleado.getSalario() + empleado.getBono());

        log.info("Step 2 - Reporte: {}", reporte);
        return reporte;
    }
}

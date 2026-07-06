package com.alera.repository;

import com.alera.model.RegistroSintomas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegistroSintomasRepository extends JpaRepository<RegistroSintomas, Long> {
    List<RegistroSintomas> findAllByOrderByFechaDescIdDesc();
    List<RegistroSintomas> findByFechaBetweenOrderByFechaAscNombreManipuladorAsc(LocalDate desde, LocalDate hasta);
    long countByFechaBetween(LocalDate desde, LocalDate hasta);
    java.util.Optional<RegistroSintomas> findByNombreManipuladorAndFecha(String nombreManipulador, LocalDate fecha);
    List<RegistroSintomas> findByFechaOrderByNombreManipuladorAsc(LocalDate fecha);
}

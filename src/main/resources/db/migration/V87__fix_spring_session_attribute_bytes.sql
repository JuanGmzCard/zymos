-- V86 creó la columna como 'attribute_value' pero Spring Session JDBC espera 'attribute_bytes'
ALTER TABLE spring_session_attributes RENAME COLUMN attribute_value TO attribute_bytes;
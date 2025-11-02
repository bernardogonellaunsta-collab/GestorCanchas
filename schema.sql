-- 1. Crear la base de datos (si no existe)
CREATE DATABASE IF NOT EXISTS gestor_deportivo;

-- 2. Usar esa base de datos
USE gestor_deportivo;

-- 3. Crear la tabla 'cancha' (si no existe)
CREATE TABLE IF NOT EXISTS `cancha` (
  `id_cancha` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `deporte` varchar(60) NOT NULL,
  `precio_por_hora` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id_cancha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 4. Crear la tabla 'cliente' (si no existe)
CREATE TABLE IF NOT EXISTS `cliente` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `telefono` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- 5. Crear la tabla 'reserva' (si no existe)
CREATE TABLE IF NOT EXISTS `reserva` (
  `id_reserva` int(11) NOT NULL AUTO_INCREMENT,
  `id_cancha` int(11) NOT NULL,
  `id_cliente` int(11) NOT NULL,
  `fecha_hora_inicio` datetime NOT NULL,
  `duracion_minutos` int(11) NOT NULL,
  `tipo` varchar(10) NOT NULL,
  `fecha_fin` date DEFAULT NULL,
  `dia_semana` varchar(15) DEFAULT NULL,
  `descuento_aplicado` double DEFAULT 0,
  `costo_total` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id_reserva`),
  KEY `id_cancha` (`id_cancha`),
  KEY `id_cliente` (`id_cliente`),
  CONSTRAINT `reserva_ibfk_1` FOREIGN KEY (`id_cancha`) REFERENCES `cancha` (`id_cancha`),
  CONSTRAINT `reserva_ibfk_2` FOREIGN KEY (`id_cliente`) REFERENCES `cliente` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
📌 Sistema de Gestión – Rectificadora Cornejo

🛠️ Descripción del proyecto

Esta aplicación es un sistema completo de gestión para una rectificadora de motores, pensado para mejorar la organización interna del taller y ofrecer a los clientes una forma clara y sencilla de seguir sus servicios.

El sistema permite:
	•	Seguimiento del estado del motor por número de orden.
	•	Consulta del estado de solicitudes de presupuesto.
	•	Generación, aprobación y seguimiento de presupuestos.
	•	Pago de seña (30%) mediante MercadoPago Sandbox.
	•	Consulta interna del taller:
	•	Órdenes de trabajo
	•	Repuestos utilizados
	•	Historial y estado del motor
	•	Sistema de login con Basic Auth para el taller.
	•	Frontend público para clientes y privado para administradores.

⸻

🚀 Tecnologías utilizadas

Backend
	•	Java 17
	•	Spring Boot
	•	Spring Web
	•	Spring Data JPA
	•	H2 / PostgreSQL
	•	MercadoPago SDK
	•	Maven

Frontend
	•	HTML5
	•	CSS3 + estilos personalizados
	•	Bootstrap 5.3
	•	JavaScript puro
	•	MercadoPago Bricks

⸻

📂 Estructura del proyecto
/src
  /main
    /java
      /domain
      /repository
      /security
      /service
      /web
        /dto
    /resources
      /db
        /migration
      /static
        /css
        /img
        /js
        /reportes
      estan todos los HTML
    application.yml

💡 Funcionalidades principales

🔹 1. Seguimiento de motor

Permite consultar el estado de una orden ingresando el número del motor.
URL: /consulta.html

⸻

🔹 2. Estado de solicitud (presupuesto)

Muestra al cliente:
	•	Descripción del problema
	•	Datos de la unidad
	•	Datos del cliente
	•	Decisión del taller
	•	Estado del pago
	•	Descarga de factura
	•	Repuestos usados (solo visible al taller)

URL: /estado-solicitud.html

⸻

🔹 3. Pagos con MercadoPago
	•	Integración con MercadoPago BRICKS
	•	Modo Sandbox
	•	Pago de seña del 30%
	•	Actualización de estados
	•	Facturación al completar el pago

⸻

🔹 4. Login del taller

Sistema simple con Basic Auth.
Permite acceder a endpoints internos del taller.
Guarda el token en localStorage si se marca “Recordarme”.

URL: /login.html

⸻

📥 Cómo ejecutar el proyecto

1. Clonar el repositorio
https://github.com/BrunelliJazmin405318/TrabajoFinalIntegrador.git

2. Levantar el backend
  mvn spring-boot:run

Backend disponible en: http://localhost:8080

3. Abrir el frontend

Abrir home.html o acceder desde navegador: http://localhost:8080/home.html

🔧 Configuración de MercadoPago

En application.yml:
MP_ACCESS_TOKEN=TEST-XXXXXXXXXXXXXXXX
En frontend:
const MP_PUBLIC_KEY = 'TEST-XXXXXXXXXXXXXXXX';

🔐 Credenciales del taller
Usuario: admin  
Contraseña: admin

👩‍💻 Autoras del proyecto

Jazmín Brunelli – Desarrollo Backend & Frontend
Victoria Ledezma – Desarrollo Backend & Frontend

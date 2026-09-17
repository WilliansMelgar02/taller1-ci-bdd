# =============================================================================
#  Imagen de ejecución del Portal de Clientes.
#
#  "Build once, deploy many": el jar se construye y se prueba UNA vez en la
#  etapa de empaquetado del pipeline, y esta imagen solo lo empaqueta. La misma
#  imagen, sin recompilar, es la que pasa por staging y la que se promueve como
#  estable; lo único que cambia entre ambientes son las variables de entorno.
#
#  Construcción (la hace el pipeline):
#    mvn package -DskipTests
#    docker build --build-arg VERSION=1.1.0-a1b2c3d --build-arg COMMIT_SHA=$(git rev-parse HEAD) \
#                 -t ghcr.io/williansmelgar02/portal-clientes:1.1.0-a1b2c3d .
# =============================================================================
FROM eclipse-temurin:17-jre-alpine

ARG VERSION=desarrollo
ARG COMMIT_SHA=local
ARG FECHA_BUILD=desconocida

# Etiquetas OCI: trazabilidad de la imagen hasta el commit que la originó.
# Los scripts de despliegue leen la versión desde aquí para verificarla.
LABEL org.opencontainers.image.title="portal-clientes" \
      org.opencontainers.image.description="Portal de Clientes - Examen Final Automatizacion de Pruebas" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${COMMIT_SHA}" \
      org.opencontainers.image.created="${FECHA_BUILD}" \
      org.opencontainers.image.source="https://github.com/WilliansMelgar02/taller1-ci-bdd"

# El proceso no corre como root: si la aplicación fuera comprometida, el
# atacante no obtiene privilegios sobre el contenedor.
RUN addgroup -S portal && adduser -S -G portal portal

WORKDIR /app
COPY --chown=portal:portal target/portal-clientes.jar portal-clientes.jar
USER portal

ENV PUERTO=8080 \
    VERSION_APP=${VERSION} \
    COMMIT_SHA=${COMMIT_SHA} \
    COLOR_DESPLIEGUE=sin-color

EXPOSE 8080

# Docker marca el contenedor como "unhealthy" si deja de responder.
HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=3 \
  CMD wget -qO- http://localhost:8080/health > /dev/null || exit 1

# Forma exec: java es el PID 1 y recibe SIGTERM de 'docker stop', lo que
# permite un cierre ordenado en lugar de cortar peticiones en curso.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/portal-clientes.jar"]

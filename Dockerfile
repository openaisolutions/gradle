FROM eclipse-temurin:17-jre
WORKDIR /app
COPY build/install/app /app
CMD ["bin/app"]

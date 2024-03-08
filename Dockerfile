FROM eclipse-temurin:11

# Add maven
RUN apt-get update && apt-get install -y maven

# Copy the pom.xml file and download dependencies
RUN mkdir -p /usr/src/zamzar-mock
COPY pom.xml /usr/src/zamzar-mock
WORKDIR /usr/src/zamzar-mock
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

# Build the JAR
COPY . /usr/src/zamzar-mock
WORKDIR /usr/src/zamzar-mock
RUN mvn --batch-mode --no-transfer-progress package

# Run JAR
CMD ["java", "-jar", "/usr/src/zamzar-mock/target/zamzar-mock-1.0.0-SNAPSHOT.jar"]

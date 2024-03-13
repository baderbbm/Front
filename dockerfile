# Ce conteneur Docker sera basé sur une image Maven avec la version 3.8 et OpenJDK 17
FROM maven:3.8-openjdk-17

# Définit le répertoire de travail à /app dans le conteneur
WORKDIR /app

# Copie le contenu du répertoire de construction local /app
COPY . /app

# Exécutez mvn install pour télécharger les dépendances
RUN mvn install

# Exécutez mvn package pour construire le JAR
RUN mvn -B package

# Définit un volume Docker pour le répertoire /tmp
VOLUME /tmp

# Spécifier le chemin du fichier JAR à copier dans le conteneur
ARG JAR_FILE=target/*.jar

# Copie le fichier JAR spécifié par l'argument JAR_FILE vers le répertoire /app dans le conteneur
COPY ${JAR_FILE} app.jar

# Définit la commande à exécuter lorsque le conteneur démarre
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

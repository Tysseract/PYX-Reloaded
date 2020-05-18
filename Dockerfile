FROM maven:3.6.3-jdk-8-alpine

LABEL maintainer="Tys vanZeyl <tys@tysseract.net>" \
  org.label-schema.name="Pretend You're Xyzzy Reloaded and Dockerized" \
  org.label-schema.vendor="Tys vanZeyl" \
  org.label-schema.schema-version="1.0"
  
EXPOSE 80

#RUN apk update && \
#  apk upgrade && \
#  apk add git && \
#  rm -rf /var/cache/apk/*
  
#RUN cd / && \
#  git clone https://github.com/Tysseract/PYX-Reloaded.git
  
COPY * /PYX-Reloaded/
  
RUN cd /PYX-Reloaded && \
  mvn clean package
  
ENTRYPOINT ["java", "-jar", "$HOME/PYX-Reloaded/target/PYX-jar-with-dependencies.jar"]
FROM maven:3.6.1-jdk-8-alpine

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
  
COPY ./ /PYX-Reloaded/

WORKDIR /PYX-Reloaded

RUN ["mvn", "clean", "package"]

#RUN ["chmod", "+x", "run.sh"]

ENTRYPOINT ["java", "-jar", "/PYX-Reloaded/target/PYX-Reloaded-jar-with-dependencies.jar"]
#ENTRYPOINT ["/PYX-Reloaded/run.sh"]
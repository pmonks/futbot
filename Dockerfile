FROM clojure:tools-deps
RUN mkdir -p /opt/futbot
WORKDIR /opt/futbot
COPY deps.edn /opt/futbot
RUN clojure -Srepro -J-Dclojure.main.report=stderr -P
COPY . /opt/futbot
RUN clojure -Srepro -J-Dclojure.main.report=stderr -M:main -c ./docker-config.edn

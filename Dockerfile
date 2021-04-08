FROM clojure:tools-deps
RUN mkdir -p /opt/futbot
WORKDIR /opt/futbot
COPY deps.edn /opt/futbot
RUN clojure -Srepro -e '(println "Dependencies downloaded")'
COPY . /opt/futbot
RUN clojure -Srepro -m futbot.main -c ./docker-config.edn

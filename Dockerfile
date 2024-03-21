FROM golang:alpine3.19
RUN go install github.com/BattlesnakeOfficial/rules/cli/battlesnake@latest
ENTRYPOINT ["battlesnake"]

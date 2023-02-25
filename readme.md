# pass4s playground

This repository contains examples for [Functional messaging in Scala with pass4s](https://blog.michalp.net/posts/scala/pass4s-basics/). It uses [pass4s](https://github.com/ocadotechnology/pass4s) library to implement the messaging layer.

The examples cover basic usage of the library. You can find the code in `./basic` and `./json`. They cover setting up producer and consumer backed by SNS/SQS using localstack.
## Localstack setup

### docker-compose

You can start localstack to run the examples with `docker-compose up`. It will spin up localstack instance along with a script that creates topic, queue and a subscription.

### resources setup
If you prefer to run the localstack on your own or use your own AWS account, here's the setup script you can adapt to spin up necessary resources.

```bash
# Executing SNS
aws sns create-topic --name local_sns
# Executing SQS
aws sqs create-queue --queue-name local_queue
# Subscribing to SNS to SQS
aws sns subscribe --attributes 'RawMessageDelivery=true' --topic-arn arn:aws:sns:eu-west-2:000000000000:local_sns --protocol sqs --notification-endpoint arn:aws:sqs:eu-west-2:000000000000:local_queue
```

Make sure to set `RawMessageDelivery=true` for the example to work correctly - this is assumed by `pass4s`.

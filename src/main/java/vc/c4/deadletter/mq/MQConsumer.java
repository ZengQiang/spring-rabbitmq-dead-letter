package vc.c4.deadletter.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import vc.c4.deadletter.config.MQConfig;
import vc.c4.deadletter.domain.ExampleObject;

@Component
public class MQConsumer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MQConsumer.class);

	// Annotation to listen for an ExampleObject
	@RabbitListener(queues = MQConfig.OUTGOING_QUEUE)
	public void handleMessage(ExampleObject exampleObject) {
		LOGGER.info("Received incoming object at {}", exampleObject.getDate());
	}

}

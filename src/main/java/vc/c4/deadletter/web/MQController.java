package vc.c4.deadletter.web;

import java.util.Arrays;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import vc.c4.deadletter.config.MQConfig;
import vc.c4.deadletter.domain.ExampleObject;

@RestController
@RequestMapping("/mq")
public class MQController {
	@Autowired
	private RabbitTemplate rabbitTemplate;

	@RequestMapping(value = "/", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Object hello() {
		return "Hello MQ";
	}

	@RequestMapping(value = "/valid", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Object valid() {
		rabbitTemplate.convertAndSend(MQConfig.OUTGOING_QUEUE, new ExampleObject());
		return "valid";
	}

	@RequestMapping(value = "/invalid", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
	public Object invalid() {
		rabbitTemplate.convertAndSend(MQConfig.OUTGOING_QUEUE, Arrays.asList(1, 2, 3));
		return "invalid";
	}
}

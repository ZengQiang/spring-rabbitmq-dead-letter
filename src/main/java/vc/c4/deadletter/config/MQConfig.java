package vc.c4.deadletter.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

@Configuration
public class MQConfig {

	public static final String OUTGOING_QUEUE = "outgoing.example";

	public static final String INCOMING_QUEUE = "incoming.example";

	@Autowired
	private ConnectionFactory cachingConnectionFactory;

	// Standardize on a single objectMapper for all message queue items
	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate outgoingSender() {
		RabbitTemplate rabbitTemplate = new RabbitTemplate(cachingConnectionFactory);
		// rabbitTemplate.setQueue(outgoingQueue().getName());
		// rabbitTemplate.setRoutingKey(outgoingQueue().getName());
		rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter());
		return rabbitTemplate;
	}

	// Setting the annotation listeners to use the jackson2JsonMessageConverter
	@Bean
	public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(cachingConnectionFactory);
		factory.setMessageConverter(jackson2JsonMessageConverter());
		factory.setAdviceChain(retryOperationsInterceptor());
		factory.setConcurrentConsumers(1);
		factory.setMaxConcurrentConsumers(10);
		factory.setDefaultRequeueRejected(false);
		factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
//		factory.setTransactionManager(transactionManager);
		factory.setChannelTransacted(true);
		return factory;
	}

	@Bean
    public RetryOperationsInterceptor retryOperationsInterceptor(){
		return RetryInterceptorBuilder.stateless()
//				.retryPolicy(simpleRetryPolicy())
				.maxAttempts(3)
				.backOffOptions(1000, 2, 10000)
				.recoverer(new RejectAndDontRequeueRecoverer())
				.build();
	}

	@Bean
	public SimpleRetryPolicy simpleRetryPolicy(){
		Map<Class<? extends Throwable> , Boolean> exceptionsMap = new HashMap<Class<? extends Throwable> , Boolean>();
		exceptionsMap.put(ListenerExecutionFailedException.class, true); //retriable
		exceptionsMap.put(AmqpRejectAndDontRequeueException.class, false);//not retriable
		SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(5 , exceptionsMap );
		return retryPolicy;
	}

	@Bean
	public Queue outgoingQueue() {
		Map<String, Object> args = new HashMap<String, Object>();
		// The default exchange
		args.put("x-dead-letter-exchange", "");
		// Route to the incoming queue when the TTL occurs
		args.put("x-dead-letter-routing-key", INCOMING_QUEUE);
		// TTL 5 seconds
		// args.put("x-message-ttl", 5000);
		return new Queue(OUTGOING_QUEUE, true, false, false, args);
	}

	@Bean
	public Queue incomingQueue() {
		return new Queue(INCOMING_QUEUE);
	}
}

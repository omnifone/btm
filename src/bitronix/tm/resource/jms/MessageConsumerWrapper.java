package bitronix.tm.resource.jms;

import bitronix.tm.resource.common.TransactionContextHelper;

import javax.jms.*;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

/**
 * {@link MessageConsumer} wrapper that adds XA enlistment semantics.
 * <p>&copy; Bitronix 2005, 2006, 2007</p>
 *
 * @author lorban
 */
public class MessageConsumerWrapper implements MessageConsumer {

    private MessageConsumer messageConsumer;
    private DualSessionWrapper session;
    private ConnectionFactoryBean bean;

    public MessageConsumerWrapper(MessageConsumer messageConsumer, DualSessionWrapper session, ConnectionFactoryBean bean) {
        this.messageConsumer = messageConsumer;
        this.session = session;
        this.bean = bean;
    }

    private MessageConsumer getMessageConsumer() {
        return messageConsumer;
    }

    /**
     * Enlist this connection into the current transaction if automaticEnlistingEnabled = true for this resource.
     * If no transaction is running then this method does nothing.
     * @throws javax.jms.JMSException
     */
    private void enlistResource() throws JMSException {
        if (bean.getAutomaticEnlistingEnabled()) {
            try {
                TransactionContextHelper.enlistInCurrentTransaction(session, bean);
            } catch (SystemException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            } catch (RollbackException ex) {
                throw (JMSException) new JMSException("error enlisting " + this).initCause(ex);
            }
        } // if getAutomaticEnlistingEnabled
    }

    public String toString() {
        return "a MessageConsumerWrapper of " + session;
    }

    /* MessageProducer with special XA semantics implementation */

    public Message receive() throws JMSException {
        enlistResource();
        return getMessageConsumer().receive();
    }

    public Message receive(long timeout) throws JMSException {
        enlistResource();
        return getMessageConsumer().receive(timeout);
    }

    public Message receiveNoWait() throws JMSException {
        enlistResource();
        return getMessageConsumer().receiveNoWait();
    }

    public void close() throws JMSException {
        // do nothing as the close is handled by the session handle
    }

    /* dumb wrapping of MessageProducer methods */

    public String getMessageSelector() throws JMSException {
        return getMessageConsumer().getMessageSelector();
    }

    public MessageListener getMessageListener() throws JMSException {
        return getMessageConsumer().getMessageListener();
    }

    public void setMessageListener(MessageListener listener) throws JMSException {
        getMessageConsumer().setMessageListener(listener);
    }

}
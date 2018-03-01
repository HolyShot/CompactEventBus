package com.sumail.shadow.eventbus;

import android.os.Handler;

import com.sumail.shadow.concurrent.HandlerExecutor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

/**
 * 时间总线-简单实现
 * @author Shadow
 * @date 2018.03.01.
 */

public class EventBus {

    // EventType -> List<Subscription>，事件到订阅对象之间的映射
    private final ConcurrentHashMap<Class<?>,Collection<Subscription>> subscriptionByEnentType = new ConcurrentHashMap<>();

    // Subscriber -> List<EventType>，订阅源到它订阅的的所有事件类型的映射
    private final ConcurrentHashMap<Object, Subscription> typesBySubscriber = new ConcurrentHashMap<Object, Subscription>();
    public static final EventBus defaultInstance = new EventBus();
    // 约定的订阅方法
    public final String eventMethod;
    public static EventBus getDefault(){

        return defaultInstance;
    }

    public EventBus(){
        this("onEvent");
    }
    public EventBus(String eventMthod){
        this.eventMethod = eventMthod;
    }
    /***
     * 注册对象满足一下条件：
     *  以约定的方法开头，或者使用@Subscribe
     */
    public void register(Object subscriber){

        register(subscriber, subscriber.getClass());
    }

    public void register(Object subscriber, Class<?> aClass) {

        if (subscriber == null)
            return;
        Subscription subscription = typesBySubscriber.get(subscriber);
        if (subscription == null){
            typesBySubscriber.putIfAbsent(subscriber,
                    subscription = new Subscription(subscriber,
                            aClass.getDeclaredMethods().length));
        }else {
            // 防止重复注册
            return;
        }

        for (Method method : aClass.getDeclaredMethods()) {
            Class<?>[] parameterTypes;
            if ((eventMethod != null && method.getName().startsWith(eventMethod)
                    || method.getAnnotation(Subscribe.class) != null)
                    && (parameterTypes = method.getParameterTypes()) != null
                    && parameterTypes.length == 1){
                Class<?> eventType = parameterTypes[0];
                regist(subscription,eventType,method);

            }
        }

    }

    /**
     * 注册指定对象的单个方法
     * @param subscriber
     * @param eventType
     * @param methodName
     */
    private void register(Object subscriber, String methodName,
                          Class<?> eventType) {
        if (subscriber == null)
            return;

           Subscription subscription = typesBySubscriber.get(subscriber);
           if (subscription == null){
               typesBySubscriber.putIfAbsent(subscriber,
                       subscription = new Subscription(subscriber,
                               subscriber.getClass().getDeclaredMethods().length));
           }
           Method m = null;
            try {
                m = subscriber.getClass().getMethod(methodName, eventType);
            }catch (Exception e){

            }
            if (m == null) {
                return;
            }
            regist(subscription, eventType, m);
    }

    public void regist(Subscription subscription, Class<?> eventType, Method m) {
        subscription.addMethod(eventType, m);

        synchronized(this){
            Collection<Subscription> subscriptionColl = subscriptionByEnentType
                    .get(eventType);
            if (subscriptionColl == null) {
                subscriptionByEnentType.put(eventType,
                        subscriptionColl = new CopyOnWriteArraySet<Subscription>());
            }
            subscriptionColl.add(subscription);
        }
    }
    /**
     * 反注册
     *
     * @param subscriber
     */
    public void unregist(Object subscriber) {
        Subscription subscription = typesBySubscriber.remove(subscriber);
        if (subscription == null) {
            return;
        }
        Collection<Class<?>> evtypes = subscription.getEventTypes();
        if (evtypes == null) {
            return;
        }
        for (Class<?> evtype : evtypes) {
            Collection<Subscription> subs = subscriptionByEnentType
                    .get(evtype);
            if (subs != null) {
                subs.remove(subscription);
            }
        }
    }

    /**
     * 发布事件，在当前线程中通知订阅者
     *
     * @param event
     *            - 事件
     */
    public void post(final Object event) {
        post(event, HandlerExecutor.getHandler());
    }

    /**
     * 判断指定对象是否已注册
     *
     * @param subcription
     * @return
     */
    public boolean containsSubcription(Object subcription) {
        return typesBySubscriber.contains(subcription);
    }

    /**
     * 返回指定Event类型已注册的子类型
     *
     * @param superType
     * @return
     */
    public List<Class<?>> eventOfType(Class<?> superType) {
        List<Class<?>> rs = new ArrayList<Class<?>>(
                subscriptionByEnentType.size() / 2);
        for (Class<?> evtType : subscriptionByEnentType.keySet()) {
            if (superType.isAssignableFrom(evtType)) {
                rs.add(evtType);
            }
        }
        return rs;
    }

    /**
     * 发布事件，使用指定的线程池通知订阅者
     *
     * @param event
     *            - 事件
     * @param ex
     *            - 线程池
     */
    public void post(final Object event, ExecutorService ex) {
        if (ex == null) {
            post(event);
            return;
        }
        if (event == null) {
            return;
        }
        final Class<?> evtype = event.getClass();
        final Collection<Subscription> subcs = subscriptionByEnentType.get(evtype);
        if (subcs == null) {
            return;
        }
        ex.submit(new Runnable() {
            public void run() {
                for (final Subscription sub : subcs) {
                    Collection<Method> ms = sub.getMethods(evtype);
                    if (ms != null) {
                        for (final Method m : ms) {
                            executeMethod(sub, event, m);
                        }
                    }
                }
            }
        });
    }

    /**
     * 发布事件，使用指定的hander通知订阅者
     *
     * @param event
     *            - 事件
     * @param ex
     *            - 线程池
     */
    public void post(final Object event, Handler ex) {
        if (ex == null) {
            post(event);
            return;
        }
        if (event == null) {
            return;
        }

        final Class<?> evtype = event.getClass();
        final Collection<Subscription> subcs = subscriptionByEnentType.get(evtype);
        if (subcs == null) {
            return;
        }

        if(Thread.currentThread().getId() == HandlerExecutor.getId()){
            for (final Subscription sub : subcs) {
                Collection<Method> ms = sub.getMethods(evtype);
                if (ms != null) {
                    for (final Method m : ms) {
                        executeMethod(sub, event, m);
                    }
                }
            }
        }else{
            ex.post(new Runnable() {
                public void run() {
                    for (final Subscription sub : subcs) {
                        Collection<Method> ms = sub.getMethods(evtype);
                        if (ms != null) {
                            for (final Method m : ms) {
                                executeMethod(sub, event, m);
                            }
                        }
                    }
                }
            });
        }
    }

    private void executeMethod(Subscription sub, final Object event,
                               final Method m) {
        try {
            m.setAccessible(true);
            m.invoke(sub.getTarger(), event);
        } catch (Exception e) {
            Throwable t = e.getCause();
            if (t != null) {
                t.printStackTrace();
            } else {
                e.printStackTrace();
            }
        }
    }
}

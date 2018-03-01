package com.sumail.shadow.eventbus;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件订阅者
 * @author Shadow
 * @date 2018.03.01.
 */

public class Subscription {
    private Object target;
    private ConcurrentHashMap<Class<?>,Collection<Method>> mConcurrentHashMap;
    public Subscription(Object target){
        this.target = target;
        mConcurrentHashMap = new ConcurrentHashMap<>();
    }
    public Subscription(Object target,int size){
        this.target = target;
        mConcurrentHashMap = new ConcurrentHashMap<>(size);
    }
    public Collection<Class<?>> getEventTypes() {
        return mConcurrentHashMap.keySet();
    }
    public Collection<Method> getMethods(Class<?> evtClass) {
        return mConcurrentHashMap.get(evtClass);
    }
    public void addMethod(Class<?> argClass,Method method){
       Collection<Method> mColl =  mConcurrentHashMap.get(argClass);
       if (mColl == null){
             mConcurrentHashMap.putIfAbsent(argClass,mColl = new HashSet<>());
       }
       mColl.add(method);
    }
    public Object getTarger(){
        return target;
    }
}

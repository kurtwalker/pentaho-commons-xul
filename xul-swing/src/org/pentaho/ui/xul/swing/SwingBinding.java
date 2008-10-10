package org.pentaho.ui.xul.swing;

import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.pentaho.ui.xul.XulComponent;
import org.pentaho.ui.xul.XulEventSource;
import org.pentaho.ui.xul.binding.BindingConvertor;
import org.pentaho.ui.xul.binding.BindingException;
import org.pentaho.ui.xul.binding.BindingUtil;
import org.pentaho.ui.xul.binding.DefaultBinding;
import org.pentaho.ui.xul.binding.BindingConvertor.Direction;
import org.pentaho.ui.xul.dom.Document;

public class SwingBinding extends DefaultBinding{

  public SwingBinding(Document document, String sourceId, String sourceAttr, String targetId, String targetAttr) {
    super(document, sourceId, sourceAttr, targetId, targetAttr);
  }

  public SwingBinding(Document document, Object source, String sourceAttr, String targetId, String targetAttr) {
    super(document, source, sourceAttr, targetId, targetAttr);
  }

  public SwingBinding(Document document, String sourceId, String sourceAttr, Object target, String targetAttr) {
    super(document, sourceId, sourceAttr, target, targetAttr);
  }
  
  public SwingBinding(Object source, String sourceAttr, Object target, String targetAttr) {
    super(source, sourceAttr, target, targetAttr);
  }
  
  @Override
  protected PropertyChangeListener setupBinding(final Reference a, final String va, final Reference b, final String vb,
      final Direction dir) {
    if (a.get() == null || va == null) {
      throw new BindingException("source bean or property is null");
    }
    if (!(a.get() instanceof XulEventSource)) {
      throw new BindingException("Binding error, source object "+a.get()+" not a XulEventSource instance");
    }
    if (b.get() == null || vb == null) {
      throw new BindingException("target bean or property is null");
    }
    Method sourceGetMethod = BindingUtil.findGetMethod(a.get(), va);

    Class getterClazz = BindingUtil.getMethodReturnType(sourceGetMethod, a.get());
    getterMethods.push(sourceGetMethod);

    //find set method
    final Method targetSetMethod = BindingUtil.findSetMethod(b.get(), vb, getterClazz);

    //setup prop change listener to handle binding
    PropertyChangeListener listener = new PropertyChangeListener() {
      public void propertyChange(final PropertyChangeEvent evt) {
        final PropertyChangeListener cThis = this;
        if (evt.getPropertyName().equalsIgnoreCase(va)) {
          try {
            Object value = evaluateExpressions(evt.getNewValue());
            final Object finalVal = doConversions(value, dir);
            if(!EventQueue.isDispatchThread() && b.get() instanceof XulComponent){
              logger.error("Binding Error! Update to XulComponenet ("+target.get()+","+targetAttr+") outside of event thread!");
              
              EventQueue.invokeLater(new Runnable(){
                public void run() {
                  try{
                    Object targetObject = b.get();
                    if(targetObject == null){
                      logger.error("Binding target was Garbage Collected, removing propListener");
                      destroyBindings();                      
                      return;
                    }
                    targetSetMethod.invoke(targetObject, finalVal);
                  } catch(InvocationTargetException e){
                    throw new BindingException("Error invoking setter method [" + targetSetMethod.getName() + "] on target: "+target, e);
                  } catch(IllegalAccessException e){
                    throw new BindingException("Error invoking setter method [" + targetSetMethod.getName() + "] on target: "+target, e);
                  }
                }
              });
            
            }
          
            Object targetObject = b.get();
            if(targetObject == null){
              logger.error("Binding target was Garbage Collected, removing propListener");
              destroyBindings();                      
              return;
            }
            logger.info("Setting val: "+finalVal+" on: "+targetObject);
            targetSetMethod.invoke(targetObject, finalVal);
          
          } catch (Exception e) {
            throw new BindingException("Error invoking setter method [" + targetSetMethod.getName() + "] on target: "+target.get(), e);
          }
        }
      }
    };
    ((XulEventSource) a.get()).addPropertyChangeListener(listener);

    return listener;
  }
  
}

  
package com.fasterxml.jackson.jr.ob.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

public final class BeanProperty
{
    protected final SerializedString _name;

    /**
     * Pre-resolved type id for reading/writing values, if statically known.
     *<p>
     * Note: yes, access is without either volatile or synchronized. But it is
     * an atomic type; so in the very worst case, modification just won't
     * stick. It will never result in invalid value being accessible.
     */
    protected int _typeId;

    /**
     * For non-trivial non-bean types
     */
    protected final ValueReader _valueReader;
    
    protected final Method _getMethod, _setMethod;

    // For serialization
    public BeanProperty(String name, int typeId, Method getMethod, Method setMethod) {
        _name = new SerializedString(name);
        _typeId = typeId;
        _getMethod = getMethod;
        _setMethod = setMethod;
        _valueReader = null;
    }

    // For deserialization
    public BeanProperty(String name, Method getMethod, Method setMethod) {
        _name = new SerializedString(name);
        _typeId = 0;
        _getMethod = getMethod;
        _setMethod = setMethod;
        _valueReader = null;
    }

    protected BeanProperty(BeanProperty src, ValueReader vr) {
        _name = src._name;
        _typeId = src._typeId;
        _getMethod = src._getMethod;
        _setMethod = src._setMethod;
        _valueReader = vr;
    }

    public BeanProperty withReader(ValueReader vr) {
        return new BeanProperty(this, vr);
    }
    
    public void overridTypeId(int id) {
        _typeId = id;
    }

    public Type genericSetterType() {
        return _setMethod.getGenericParameterTypes()[0];
    }

    public Class<?> rawSetterType() {
        return _setMethod.getParameterTypes()[0];
    }
    
    public ValueReader getReader() { return _valueReader; }
    
    public int getTypeId() { return _typeId; }

    public SerializedString getName() { return _name; }
    
    public SerializedString getNameIfHasSetter() {
        return (_setMethod == null) ? null : _name;
    }
    
    public Object getValueFor(Object bean) throws IOException
    {
        if (_getMethod == null) {
            throw new IllegalStateException("No getter for property '"+_name+"' (type "+_bean()+")");
        }
        try {
            return _getMethod.invoke(bean);
        } catch (Exception e) {
            throw new JSONObjectException("Failed to access property '"+_name+"'; exception "+e.getClass().getName()+"): "
                    +e.getMessage(), e);
        }
    }

    public Object setValueFor(Object bean, Object value) throws IOException
    {
        if (_setMethod == null) {
            throw new IllegalStateException("No setter for property '"+_name+"' (type "+_bean()+")");
        }
        try {
            return _setMethod.invoke(bean, value);
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            throw new JSONObjectException("Failed to set property '"+_name+"'; exception "+e.getClass().getName()+"): "
                    +e.getMessage(), e);
        }
    }

    protected String _bean() {
        Class<?> cls;
        if (_getMethod != null) {
            cls = _getMethod.getDeclaringClass();
        } else if (_setMethod != null) {
            cls = _setMethod.getDeclaringClass();
        } else {
            return "UNKNOWN";
        }
        return cls.getName();
    }
}

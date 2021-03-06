package org.javarosa.core.util.externalizable;

import org.javarosa.core.util.OrderedHashtable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;

//map of objects where elements are multiple types, keys are still assumed to be of a single (non-polymorphic) type
//if elements are compound types (i.e., need wrappers), they must be pre-wrapped before invoking this wrapper, because... come on now.
public class ExtWrapMapPoly extends ExternalizableWrapper {
    private ExternalizableWrapper keyType;
    public boolean ordered;

    /* serialization */

    public ExtWrapMapPoly(Hashtable val) {
        this(val, null);
    }

    public ExtWrapMapPoly(Hashtable val, ExternalizableWrapper keyType) {
        if (val == null) {
            throw new NullPointerException();
        }

        this.val = val;
        this.keyType = keyType;
        this.ordered = (val instanceof OrderedHashtable);
    }

    /* deserialization */

    public ExtWrapMapPoly() {

    }

    public ExtWrapMapPoly(Class keyType) {
        this(keyType, false);
    }

    public ExtWrapMapPoly(ExternalizableWrapper keyType) {
        this(keyType, false);
    }

    public ExtWrapMapPoly(Class keyType, boolean ordered) {
        this(new ExtWrapBase(keyType), ordered);
    }

    public ExtWrapMapPoly(ExternalizableWrapper keyType, boolean ordered) {
        if (keyType == null) {
            throw new NullPointerException();
        }

        this.keyType = keyType;
        this.ordered = ordered;
    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapMapPoly((Hashtable)val, keyType);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        long size = ExtUtil.readNumeric(in);
        Hashtable<Object, Object> h = ordered ? new OrderedHashtable<>((int)size) : new Hashtable<>((int)size);
        for (int i = 0; i < size; i++) {
            Object key = ExtUtil.read(in, keyType, pf);
            Object elem = ExtUtil.read(in, new ExtWrapTagged(), pf);
            h.put(key, elem);
        }

        val = h;
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        Hashtable h = (Hashtable)val;

        ExtUtil.writeNumeric(out, h.size());
        for (Enumeration e = h.keys(); e.hasMoreElements(); ) {
            Object key = e.nextElement();
            Object elem = h.get(key);

            ExtUtil.write(out, keyType == null ? key : keyType.clone(key));
            ExtUtil.write(out, new ExtWrapTagged(elem));
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        ordered = ExtUtil.readBool(in);
        keyType = ExtWrapTagged.readTag(in, pf);
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) throws IOException {
        Hashtable h = (Hashtable)val;
        Object keyTagObj;

        ExtUtil.writeBool(out, ordered);

        keyTagObj = (keyType == null ? (h.size() == 0 ? new Object() : h.keys().nextElement()) : keyType);
        ExtWrapTagged.writeTag(out, keyTagObj);
    }
}

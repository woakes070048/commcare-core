package org.javarosa.core.util.externalizable;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;


//list of objects of single (non-polymorphic) type
public class ExtWrapList extends ExternalizableWrapper {
    public ExternalizableWrapper type;
    private boolean sealed;

    /* serialization */

    public ExtWrapList(Vector val) {
        this(val, null);
    }

    public ExtWrapList(Vector val, ExternalizableWrapper type) {
        if (val == null) {
            throw new NullPointerException();
        }

        this.val = val;
        this.type = type;
    }

    /* deserialization */

    public ExtWrapList() {

    }

    public ExtWrapList(Class type) {
        this(type, false);
    }

    public ExtWrapList(Class type, boolean sealed) {
        this.type = new ExtWrapBase(type);
        this.sealed = sealed;
    }

    public ExtWrapList(ExternalizableWrapper type) {
        if (type == null) {
            throw new NullPointerException();
        }

        this.type = type;
    }

    @Override
    public ExternalizableWrapper clone(Object val) {
        return new ExtWrapList((Vector)val, type);
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        if (!sealed) {
            int size = (int)ExtUtil.readNumeric(in);
            Vector<Object> v = new Vector<>(size);
            for (int i = 0; i < size; i++) {
                v.addElement(ExtUtil.read(in, type, pf));
            }
            val = v;
        } else {
            int size = (int)ExtUtil.readNumeric(in);
            Object[] theval = new Object[size];
            for (int i = 0; i < size; i++) {
                theval[i] = ExtUtil.read(in, type, pf);
            }
            val = theval;
        }
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        Vector v = (Vector)val;

        ExtUtil.writeNumeric(out, v.size());
        for (int i = 0; i < v.size(); i++) {
            ExtUtil.write(out, type == null ? v.elementAt(i) : type.clone(v.elementAt(i)));
        }
    }

    @Override
    public void metaReadExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        type = ExtWrapTagged.readTag(in, pf);
    }

    @Override
    public void metaWriteExternal(DataOutputStream out) throws IOException {
        Vector v = (Vector)val;
        Object tagObj;

        if (type == null) {
            if (v.size() == 0) {
                tagObj = new Object();
            } else {
                tagObj = v.elementAt(0);
            }
        } else {
            tagObj = type;
        }

        ExtWrapTagged.writeTag(out, tagObj);
    }
}

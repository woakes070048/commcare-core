package org.javarosa.form.api;

import org.javarosa.core.model.FormIndex;
import org.javarosa.core.model.instance.TreeReference;
import org.javarosa.core.util.externalizable.DeserializationException;
import org.javarosa.core.util.externalizable.ExtUtil;
import org.javarosa.core.util.externalizable.ExtWrapList;
import org.javarosa.core.util.externalizable.Externalizable;
import org.javarosa.core.util.externalizable.PrototypeFactory;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Records form entry actions, associating question references with user (string)
 * answers. Updating an answer does not change its ordering in the action list.
 *
 * @author Phillip Mates (pmates@dimagi.com).
 */
public class FormEntrySession implements FormEntrySessionRecorder, Externalizable {

    private Vector<FormEntryAction> actions = new Vector<>();
    private String sessionStopRef;

    /**
     * For Externalization
     */
    public FormEntrySession() {
    }

    @Override
    public void addNewRepeat(FormIndex formIndex) {
        final String questionRefString = formIndex.getReference().toString();
        int insertIndex = removeDuplicateAction(questionRefString);
        actions.insertElementAt(FormEntryAction.buildNewRepeatAction(questionRefString), insertIndex);
    }

    private int removeDuplicateAction(String questionRefString) {
        for (int i = actions.size() - 1; i >= 0; i--) {
            if (actions.elementAt(i).getQuestionRefString().equals(questionRefString)) {
                actions.removeElementAt(i);
                return i;
            }
        }
        return actions.size();
    }

    @Override
    public void addValueSet(FormIndex formIndex, String value) {
        final String questionRefString = formIndex.getReference().toString();
        int insertIndex = removeDuplicateAction(questionRefString);
        actions.insertElementAt(FormEntryAction.buildValueSetAction(questionRefString, value), insertIndex);
    }

    @Override
    public void addQuestionSkip(FormIndex formIndex) {
        final String questionRefString = formIndex.getReference().toString();
        int insertIndex = removeDuplicateAction(questionRefString);
        actions.insertElementAt(FormEntryAction.buildSkipAction(questionRefString), insertIndex);
    }

    public FormEntryAction peekAction() {
        if (actions.size() > 0) {
            return actions.elementAt(0);
        } else {
            return FormEntryAction.buildNullAction();
        }
    }

    /**
     * @return the ref path for the last action in this form entry session (e.g. where the user
     * stopped form entry)
     */
    public String getStopRef() {
        return this.sessionStopRef;
    }

    private static String computeStopRef(Vector<FormEntryAction> actions) {
        return actions.elementAt(actions.size() - 1).getQuestionRefString();
    }

    /**
     * Remove and return the FormEntryAction corresponding to the given FormIndex, if there is
     * one in this session
     */
    public FormEntryAction getAndRemoveActionForRef(TreeReference questionRef) {
        for (int i = 0; i < actions.size(); i++) {
            FormEntryAction action = actions.elementAt(i);
            if (action.getQuestionRefString().equals(questionRef.toString())) {
                actions.removeElementAt(i);
                return action;
            }
        }
        return null;
    }

    /**
     * Returns whether a NEW_REPEAT action exists for this questionRef, and removes it if it does
     */
    public boolean getAndRemoveRepeatActionForRef(TreeReference questionRef) {
        for (FormEntryAction action : actions) {
            if (action.isNewRepeatAction() &&
                    action.getQuestionRefString().equals(questionRef.toString())) {
                return actions.removeElement(action);
            }
        }
        return false;
    }

    public int size() {
        return actions.size();
    }

    @Override
    public String toString() {
        StringBuilder sessionStringBuilder = new StringBuilder();

        for (FormEntryAction formEntryAction : actions) {
            sessionStringBuilder.append(formEntryAction).append(" ");
        }

        return sessionStringBuilder.toString().trim();
    }

    public static FormEntrySession fromString(String sessionString) {
        FormEntrySession formEntrySession = new FormEntrySession();
        for (String actionString : splitTopParens(sessionString)) {
            formEntrySession.actions.addElement(FormEntryAction.fromString(actionString));
        }

        formEntrySession.sessionStopRef = computeStopRef(formEntrySession.actions);
        return formEntrySession;
    }

    public static Vector<String> splitTopParens(String sessionString) {
        boolean wasEscapeChar = false;
        int parenDepth = 0;
        int topParenStart = 0;
        Vector<String> tokens = new Vector<>();

        for (int i = 0, n = sessionString.length(); i < n; i++) {
            char c = sessionString.charAt(i);
            if (c == '\\') {
                wasEscapeChar = !wasEscapeChar;
            } else if ((c == '(' || c == ')') && wasEscapeChar) {
                wasEscapeChar = false;
            } else if (c == '(') {
                parenDepth++;
                if (parenDepth == 1) {
                    topParenStart = i;
                }
            } else if (c == ')') {
                if (parenDepth == 1) {
                    tokens.addElement(sessionString.substring(topParenStart, i + 1));
                }
                parenDepth--;
            }
        }

        return tokens;
    }

    @Override
    public void readExternal(DataInputStream in, PrototypeFactory pf) throws IOException, DeserializationException {
        actions = (Vector<FormEntryAction>)ExtUtil.read(in, new ExtWrapList(FormEntryAction.class), pf);
        sessionStopRef = computeStopRef(actions);
    }

    @Override
    public void writeExternal(DataOutputStream out) throws IOException {
        ExtUtil.write(out, new ExtWrapList(actions));
    }
}

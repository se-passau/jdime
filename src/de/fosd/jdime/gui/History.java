package de.fosd.jdime.gui;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import com.sun.javafx.collections.ObservableListWrapper;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * A history of GUI <code>State</code>s. The <code>History</code> stores an index into the list of stored
 * <code>State</code>s it represents. One additional <code>State</code> is stored representing the state of the GUI
 * before a state from the <code>History</code> was loaded. This <code>State</code> can be thought of as being at the
 * index returned by {@link #getSize()}. The <code>applyX</code> methods advance/regress the index by one and apply
 * the <code>State</code> at the new index to the <code>GUI</code>.
 */
public class History {

    private static final Logger LOG = Logger.getLogger(History.class.getCanonicalName());
    private static XStream serializer;

    static {
        serializer = new XStream();
        serializer.registerConverter(new Converter() {

            private Converter c = serializer.getConverterLookup().lookupConverterForType(ArrayList.class);

            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                c.marshal(new ArrayList<>((Collection<?>) source), writer, context);
            }

            @Override
            public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
                return FXCollections.observableArrayList((Collection<?>) c.unmarshal(reader, context));
            }

            @Override
            public boolean canConvert(Class type) {
                return type.equals(ObservableListWrapper.class);
            }
        });

        serializer.alias("root", ObservableListWrapper.class);

        serializer.omitField(State.class, "treeViewTabs");
        serializer.addImplicitCollection(State.class, "output");
        serializer.alias("state", State.class);
    }

    private IntegerProperty index;
    private SimpleListProperty<State> history;
    private State inProgress;

    private ReadOnlyBooleanProperty hasPrevious;
    private ReadOnlyBooleanProperty hasNext;

    /**
     * Constructs a new <code>History</code>.
     */
    public History() {
        this.index = new SimpleIntegerProperty(0);
        this.history = new SimpleListProperty<>(FXCollections.observableArrayList());

        BooleanProperty prevProperty = new SimpleBooleanProperty();
        prevProperty.bind(history.emptyProperty().or(index.isEqualTo(0)).not());

        BooleanProperty nextProperty = new SimpleBooleanProperty();
        nextProperty.bind(history.emptyProperty().or(index.greaterThanOrEqualTo(history.sizeProperty())).not());

        this.hasPrevious = prevProperty;
        this.hasNext = nextProperty;
    }

    /**
     * If possible, advances the index by one and applies the new <code>State</code> to the <code>GUI</code>.
     * If the new index is the size of the list the previously stored in-progress <code>State</code> of the
     * <code>GUI</code> is applied.
     *
     * @param gui
     *         the <code>GUI</code> to which the <code>State</code> is to be applied
     */
    public void applyNext(GUI gui) {
        if (getIndex() == getSize()) {
            return;
        }

        index.setValue(getIndex() + 1);

        if (getIndex() == getSize()) {
            inProgress.applyTo(gui);
        } else {
            history.get(getIndex()).applyTo(gui);
        }
    }

    /**
     * If possible, regresses the index by one and applies the new <code>State</code> to the <code>GUI</code>.
     * If the index was the size of the list (indicating that the current <code>State</code> of the <code>GUI</code>
     * if not one of the archived states) the current <code>State</code> is stored.
     *
     * @param gui
     *         the <code>GUI</code> to which the <code>State</code> is to be applied
     */
    public void applyPrevious(GUI gui) {
        if (getIndex() == 0) {
            return;
        }

        if (getIndex() == getSize()) {
            inProgress = State.of(gui);
        }

        index.setValue(getIndex() - 1);
        history.get(getIndex()).applyTo(gui);
    }

    /**
     * Adds the current state of the <code>GUI</code> to the <code>History</code> and sets the index to the size of the
     * list (indicating that the current state of the GUI is not one of the archived states).
     *
     * @param gui
     *         the <code>GUI</code> whose <code>State</code>s are to be saved
     */
    public void storeCurrent(GUI gui) {
        State currentState = State.of(gui);

        if (history.isEmpty() || !history.get(getSize() - 1).equals(currentState)) {
            history.add(currentState);
            index.setValue(getSize());
        }
    }

    /**
     * Optionally (if the deserialization is successful) loads a <code>History</code> from the given <code>File</code>.
     *
     * @param file
     *         the <code>File</code> to load the <code>History</code> from
     * @return optionally the loaded <code>History</code>
     * @throws IOException
     *         if there is an <code>IOException</code> accessing the <code>file</code>
     */
    public static Optional<History> load(File file) throws IOException {
        try (InputStream is = new BufferedInputStream(new FileInputStream(file))) {
            return load(is);
        }
    }

    /**
     * Optionally (if the deserialization is successful) loads a <code>History</code> from the given
     * <code>InputStream</code>.
     *
     * @param stream
     *         the <code>InputStream</code> to load the <code>History</code> from
     * @return optionally the loaded <code>History</code>
     */
    public static Optional<History> load(InputStream stream) {
        History history;

        try {
            history = new History();
            history.history.setAll((Collection<? extends State>) serializer.fromXML(stream));
            history.history.forEach(state -> {
                GraphvizParser p = new GraphvizParser(state.getOutput());
                state.setTreeViewTabs(p.call().stream().map(GUI::getTreeTableViewTab).collect(Collectors.toList()));
            });
        } catch (XStreamException | ClassCastException e) {
            LOG.log(Level.WARNING, e, () -> "History deserialization failed.");
            return Optional.empty();
        }

        return Optional.of(history);
    }

    /**
     * Stores this <code>History</code> in serialized form in the given <code>File</code>. If <code>file</code> exists
     * it will be overridden.
     *
     * @param file
     *         the <code>File</code> to store this <code>History</code> in
     * @throws IOException
     *         if an <code>IOException</code> occurs accessing the file
     */
    public void store(File file) throws IOException {

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
            store(os);
        }
    }

    /**
     * Writes this <code>History</code> in serialized form to the given <code>OutputStream</code>.
     *
     * @param stream
     *         the <code>OutputStream</code> to write to
     */
    public void store(OutputStream stream) {
        serializer.toXML(history.get(), stream);
    }

    /**
     * Returns the number of saved <code>State</code>s. This does not include the 'in Progress' state of the GUI.
     *
     * @return the size of the <code>History</code>
     */
    public int getSize() {
        return history.size();
    }

    /**
     * Returns the size property.
     *
     * @return the size property
     */
    public ReadOnlyIntegerProperty sizeProperty() {
        return history.sizeProperty();
    }

    /**
     * Returns the current index. If this method returns {@link #getSize()} the index is pointing to the 'in Progress'
     * state of the GUI.
     *
     * @return the current index
     */
    public int getIndex() {
        return index.get();
    }

    /**
     * Returns the index property.
     *
     * @return the index property
     */
    public IntegerProperty indexProperty() {
        return index;
    }

    /**
     * Returns whether there is a <code>State</code> before the one currently pointed at by the index.
     *
     * @return whether there is a previous <code>State</code>
     */
    public boolean hasPrevious() {
        return hasPrevious.get();
    }

    /**
     * Returns the previous property.
     *
     * @return the previous property
     */
    public ReadOnlyBooleanProperty hasPreviousProperty() {
        return hasPrevious;
    }

    /**
     * Returns whether there is a <code>State</code> (including the 'in Progress' state) after the one currently
     * pointed at by the index.
     *
     * @return whether there is a next <code>State</code>
     */
    public boolean hasNext() {
        return hasNext.get();
    }

    /**
     * Returns the next property.
     *
     * @return the next property
     */
    public ReadOnlyBooleanProperty hasNextProperty() {
        return hasNext;
    }
}

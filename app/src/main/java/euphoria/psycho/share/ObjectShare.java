package euphoria.psycho.share;


public class ObjectShare {
    public static <T> T checkNotNull(T object) {
        if (object == null) throw new NullPointerException();
        return object;
    }

}

public class ParseException extends Exception {

    private String info;

    public ParseException(String info) {
        this.info = info;
    }

    @Override
    public String getMessage() {
        return "Parsing of the code has failed! " + info;
    }

}

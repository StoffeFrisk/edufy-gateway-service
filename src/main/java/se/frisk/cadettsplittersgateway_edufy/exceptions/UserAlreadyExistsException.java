package se.frisk.cadettsplittersgateway_edufy.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAlreadyExistsException extends RuntimeException {
    private String object;
    private String field;
    private Object value;

    public UserAlreadyExistsException(String object, String field, Object value) {
        super(String.format("%s with %s '%s' already exists", object, field, value));
        this.object = object;
        this.field = field;
        this.value = value;
    }
}

package ws.palladian.preprocessing.featureextraction;

import java.util.regex.Pattern;

public class RegExTokenRemover extends TokenRemover {
    
    private Pattern pattern;

    public RegExTokenRemover(Pattern pattern) {
        this.pattern = pattern;
    }
    
    public RegExTokenRemover(String pattern) {
        this(Pattern.compile(pattern));
    }
    
    @Override
    protected boolean remove(Annotation annotation) {
        return pattern.matcher(annotation.getValue()).matches();
    }

}

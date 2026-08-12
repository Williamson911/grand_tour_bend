package be.technifutur.grandtourbend;

import lombok.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@EqualsAndHashCode @ToString
public class SearchParam<T>{

    @Getter @Setter
    private String field;
    @Getter @Setter
    private SearchOperator op;
    @Getter @Setter
    private Object value;

    private static <T> SearchParam<T> create(Map.Entry<String,String> entry) {
        String field;
        SearchOperator op;
        String value;

        String[] key = entry.getKey().split("_");
        if(key.length == 1) {
            field = key[0];
            op = SearchOperator.EQ;
        } else {
            op = SearchOperator.valueOf(key[0].toUpperCase());
            field = key[1];
        }
        value = entry.getValue();

        return new SearchParam<T>(field,op,value);
    }

    protected static <T> List<SearchParam<T>> create(Map<String,String> routeParams) {
        return routeParams.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty() && !entry.getKey().equals("page") && !entry.getKey().equals("size") && !entry.getKey().equals("sort"))
                .map(SearchParam::<T>create)
                .toList();
    }
}
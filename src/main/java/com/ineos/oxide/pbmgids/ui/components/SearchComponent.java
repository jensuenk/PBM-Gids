package com.ineos.oxide.pbmgids.ui.components;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;

/**
 * Reusable search component with autocomplete functionality.
 * Provides search suggestions and highlighted matches.
 */
public class SearchComponent extends ComboBox<String> {

    private String currentFilter = "";
    private Function<String, List<String>> suggestionProvider;
    private Consumer<String> searchHandler;

    public SearchComponent(String placeholder) {
        setupComponent(placeholder);
        setupBehavior();
    }

    private void setupComponent(String placeholder) {
        setPlaceholder(placeholder);
        setClearButtonVisible(true);
        setWidthFull();
        setAllowCustomValue(true);
        setRenderer(new ComponentRenderer<>(this::createHighlightedSuggestion));
    }

    private void setupBehavior() {
        // Handle custom values (typed directly)
        addCustomValueSetListener(event -> {
            String customValue = event.getDetail();
            setValue(customValue);
            performSearch(customValue);
        });

        // Handle value changes (selection from dropdown)
        addValueChangeListener(event -> performSearch(event.getValue()));

        // Setup lazy loading for suggestions
        setItems(query -> {
            String filter = query.getFilter().orElse("");
            currentFilter = filter;

            if (filter.length() < 2 || suggestionProvider == null) {
                return java.util.stream.Stream.empty();
            }

            List<String> suggestions = suggestionProvider.apply(filter);
            return suggestions.stream()
                    .skip(query.getOffset())
                    .limit(query.getLimit());
        });
    }

    private Span createHighlightedSuggestion(String suggestion) {
        if (currentFilter == null || currentFilter.isEmpty() || suggestion == null) {
            return new Span(suggestion);
        }

        Span container = new Span();
        String lowerSuggestion = suggestion.toLowerCase();
        String lowerFilter = currentFilter.toLowerCase();
        int startIndex = lowerSuggestion.indexOf(lowerFilter);

        if (startIndex == -1) {
            container.setText(suggestion);
        } else {
            // Create highlighted text
            if (startIndex > 0) {
                container.add(new Span(suggestion.substring(0, startIndex)));
            }

            Span boldMatch = new Span(suggestion.substring(startIndex, startIndex + currentFilter.length()));
            boldMatch.getStyle().set("font-weight", "bold");
            container.add(boldMatch);

            if (startIndex + currentFilter.length() < suggestion.length()) {
                container.add(new Span(suggestion.substring(startIndex + currentFilter.length())));
            }
        }

        return container;
    }

    private void performSearch(String searchTerm) {
        if (searchHandler != null) {
            searchHandler.accept(searchTerm);
        }
    }

    // Public API

    public void setSuggestionProvider(Function<String, List<String>> provider) {
        this.suggestionProvider = provider;
    }

    public void setSearchHandler(Consumer<String> handler) {
        this.searchHandler = handler;
    }

    public String getCurrentFilter() {
        return currentFilter;
    }
}
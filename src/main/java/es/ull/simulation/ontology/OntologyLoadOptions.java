package es.ull.simulation.ontology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;

public final class OntologyLoadOptions {

    private final OWLOntologyLoaderConfiguration owlConfig;
    private final List<String> localMappings;

    private OntologyLoadOptions(Builder b) {
        this.owlConfig = b.owlConfig;
        this.localMappings = List.copyOf(b.localMappings);
    }

    public OWLOntologyLoaderConfiguration getOwlConfig() {
        return owlConfig;
    }

    public List<String> getLocalMappings() {
        return localMappings;
    }

    public static OntologyLoadOptions defaults() {
        return new Builder().build();
    }

    public static final class Builder {
        private OWLOntologyLoaderConfiguration owlConfig =
                new OWLOntologyLoaderConfiguration();

        private final List<String> localMappings = new ArrayList<>();

        public Builder owlConfig(OWLOntologyLoaderConfiguration cfg) {
            this.owlConfig = Objects.requireNonNull(cfg);
            return this;
        }

        public Builder addLocalMapping(String mapping) {
            this.localMappings.add(Objects.requireNonNull(mapping));
            return this;
        }

        public Builder addLocalMappings(Collection<String> mappings) {
            this.localMappings.clear();
            this.localMappings.addAll(Objects.requireNonNull(mappings));
            return this;
        }

        public OntologyLoadOptions build() {
            return new OntologyLoadOptions(this);
        }
    }
}


package growzapp.backend.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public abstract class IgnoreHibernatePropertiesMixin {
}
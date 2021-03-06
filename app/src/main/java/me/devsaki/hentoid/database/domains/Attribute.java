package me.devsaki.hentoid.database.domains;

import androidx.annotation.NonNull;

import java.io.DataInputStream;
import java.io.IOException;

import javax.annotation.Nonnull;

import io.objectbox.annotation.Backlink;
import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Index;
import io.objectbox.annotation.Transient;
import io.objectbox.relation.ToMany;
import me.devsaki.hentoid.enums.AttributeType;
import me.devsaki.hentoid.enums.Site;
import timber.log.Timber;

/**
 * Created by DevSaki on 09/05/2015.
 * Attribute builder
 */
@Entity
public class Attribute {

    @Id
    private long id;
    @Index
    private String name;
    @Index
    @Convert(converter = AttributeType.AttributeTypeConverter.class, dbType = Integer.class)
    private AttributeType type;
    @Backlink(to = "attribute")
    private ToMany<AttributeLocation> locations; // One entry per site

    // Runtime attributes; no need to expose them nor to persist them
    @Transient
    private int count;
    @Transient
    private int externalId = 0;
    @Backlink(to = "attributes") // backed by the to-many relation in Content
    public ToMany<Content> contents;


    public Attribute() {
    } // No-arg constructor required by ObjectBox

    public Attribute(@Nonnull AttributeType type, @Nonnull String name) {
        this.type = type;
        this.name = name;
    }

    public Attribute(@Nonnull AttributeType type, @Nonnull String name, @Nonnull String url, @Nonnull Site site) {
        this.type = type;
        this.name = name;
        computeLocation(site, url);
    }

    public Attribute(@Nonnull DataInputStream input) throws IOException {
        input.readInt(); // file version
        name = input.readUTF();
        type = AttributeType.searchByCode(input.readInt());
        count = input.readInt();
        externalId = input.readInt();
        int nbLocations = input.readInt();
        for (int i = 0; i < nbLocations; i++) locations.add(new AttributeLocation(input));
    }

    public long getId() {
        return (0 == externalId) ? this.id : this.externalId;
    }

    public Attribute setId(long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(@Nonnull String name) {
        this.name = name;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(@Nonnull AttributeType type) {
        this.type = type;
    }

    public ToMany<AttributeLocation> getLocations() {
        return locations;
    }

    public void setLocations(ToMany<AttributeLocation> locations) {
        this.locations = locations;
    }

    public int getCount() {
        return count;
    }

    public Attribute setCount(int count) {
        this.count = count;
        return this;
    }

    public Attribute setExternalId(int id) {
        this.externalId = id;
        return this;
    }

    Attribute computeLocation(Site site, String url) {
        locations.add(new AttributeLocation(site, url));
        return this;
    }

    public void addLocationsFrom(Attribute sourceAttribute) {
        for (AttributeLocation sourceLocation : sourceAttribute.getLocations()) {
            boolean foundSite = false;
            for (AttributeLocation loc : this.locations) {
                if (sourceLocation.site.equals(loc.site)) {
                    foundSite = true;
                    if (!sourceLocation.url.equals(loc.url))
                        Timber.w("'%s' Attribute location mismatch : current '%s' vs. add's target '%s'", this.name, loc.url, sourceLocation.url);
                    break;
                }
            }
            if (!foundSite) this.locations.add(sourceLocation);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }

    public String formatLabel(boolean useNamespace) {
        return String.format("%s%s %s", useNamespace ? type.getDisplayName().toLowerCase() + ":" : "", getName(), getCount() > 0 ? "(" + getCount() + ")" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if ((externalId != 0 && attribute.externalId != 0) && externalId != attribute.externalId)
            return false;
        if ((id != 0 && attribute.id != 0) && id != attribute.id) return false;
        if (!name.equals(attribute.name)) return false;
        return type == attribute.type;
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + externalId;
        return result;
    }
}

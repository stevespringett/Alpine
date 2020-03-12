package alpine.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;
import javax.jdo.annotations.Unique;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.UUID;

/**
 * Persistable object representing a OpenID Connect group mapped to a Team.
 *
 * @since 1.8.0
 */
@PersistenceCapable
@JsonInclude(JsonInclude.Include.NON_NULL)
@Unique(members = {"team", "groupName"})
public class MappedOidcGroup {

    @PrimaryKey
    @Persistent(valueStrategy = IdGeneratorStrategy.NATIVE)
    @JsonIgnore
    private long id;

    @Persistent(defaultFetchGroup = "true")
    @Column(name = "TEAM_ID", allowsNull = "false")
    @JsonIgnore
    private Team team;

    @Persistent
    @Column(name = "GROUP_NAME", jdbcType = "VARCHAR", length = 1024, allowsNull = "false")
    @NotBlank
    @Size(min = 1, max = 255)
    @Pattern(regexp = "[\\P{Cc}]+", message = "The group name must not contain control characters")
    private String groupName;

    @Persistent(customValueStrategy = "uuid")
    @Unique(name = "MAPPEDOIDCGROUP_UUID_IDX")
    @Column(name = "UUID", jdbcType = "VARCHAR", length = 36, allowsNull = "false")
    @NotNull
    private UUID uuid;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(final Team team) {
        this.team = team;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(final String groupName) {
        this.groupName = groupName;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(final UUID uuid) {
        this.uuid = uuid;
    }

}

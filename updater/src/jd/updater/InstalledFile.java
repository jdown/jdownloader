package jd.updater;

public class InstalledFile {
    private String relPath;
    private long   lastMod;

    public InstalledFile() {

    }

    public InstalledFile(final String rel, final long lastMod) {
        this.lastMod = lastMod;
        relPath = rel;
    }

    // required to remove dupes
    @Override
    public boolean equals(final Object o) {
        if (o == null || !(o instanceof InstalledFile)) { return false; }
        return hashCode() == o.hashCode();
    }

    public long getLastMod() {
        return lastMod;
    }

    public String getRelPath() {
        return relPath;
    }

    // required to remove dupes
    @Override
    public int hashCode() {
        return relPath.hashCode();
    }

    public void setLastMod(final long lastMod) {
        this.lastMod = lastMod;
    }

    public void setRelPath(final String relPath) {
        this.relPath = relPath;
    }
}

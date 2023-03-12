package main.osu.beatmap.parser.sections;

import main.osu.beatmap.BeatmapData;

/**
 * A parser for parsing a beatmap's metadata section.
 */
public class BeatmapMetadataParser extends BeatmapKeyValueSectionParser {
    @Override
    public boolean parse(BeatmapData data, String line) {
        String[] p = splitProperty(line);

        switch (p[0]) {
            case "Title":
                data.metadata.title = p[1];
                break;
            case "TitleUnicode":
                data.metadata.titleUnicode = p[1];
                break;
            case "Artist":
                data.metadata.artist = p[1];
                break;
            case "ArtistUnicode":
                data.metadata.artistUnicode = p[1];
                break;
            case "Creator":
                data.metadata.creator = p[1];
                break;
            case "Version":
                data.metadata.version = p[1];
                break;
            case "Source":
                data.metadata.source = p[1];
                break;
            case "Tags":
                data.metadata.tags = p[1];
                break;
        }

        return true;
    }
}

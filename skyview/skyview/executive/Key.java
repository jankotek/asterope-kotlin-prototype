package skyview.executive;

import java.util.HashMap;
import java.util.Map;

/**
 * Keys used in settings
 */
public enum Key {
    output, FilePrefix, HealPixArray,
    _nullvalues, surveyfinder, purgecache,
    _cachedfile,  settings, survey,
    contour, position, lon,  lat,  rgb, scale,
    UseDSSWCS, CopyWCS,
     _CRPIX1, _CRPIX2, _CRVAL1, _CRVAL2,
    _CDELT1, _CDELT2, _CD1_1, _CD1_2, _CD2_1, _CD2_2,
     _currentSurvey, ErrorMsg,  Ebins, subset,
    GeometryTwin,  StrictGeometry, NullImages, nofits,
    SubsetX, Mosaicker, Preprocessor, Deedger, Postprocessor,
    float_, samp, Compress, output_fits, equinox, coordinates,  resolver,
    ReqYPos, ReqXPos,  size, RefCoords, rotation,
    offset, SpellSuffix, SpellPrefix, LocalURL, cache, SaveBySurvey, shortname,
    SIATimeout, SiapURL, SIAPCoordinates, SIAPProjection, SIAPNaxis, SIAPScaling,
    SIAPFilterValue, SIAPFilterField, key, NEAT_REGION, grid, CatalogColumns,
    ExposureFinder, nonormalize, BackupSurvey, drawAngle, plotAngle,
    draw, SubsetY, ExposureFileMatch, ExposureFileGen, cornersonly, checknans,
    ClipIntensive, ClipDrizzle, ToastGrid, DefaultSIASurveys, file, vcoord, pixely,
    pixelx, pixels,  SFACTR, iscaln, scaling, imgree, imredd, imblue,  maproj,
    projection, resamp, sampler, griddd,  catalog, equinx, scoord,
    quicklook, return_, coltab, lut, GalleryXSLT, userfile, level,  Subdiv,
    SettingsUpdaters, XXX, yyy, contourSmooth, noContourPrint, smooth,
     _totalCatalogCount,
     invert,
    max, min, plotcolor, plotfontsize, plotscale,   rgbsmooth,
    ExposureKeyword, surveymanifest, surveyxml, xmlroot,  onlinetext,
    HeaderTemplate, FooterTemplate, _ImageXSize, _ImageYSize, _ImageYPixel, _ImageXPixel,
      requested_coords, _meta_copyright, _meta_provenance, webrootpath,
    _catalogFile, _output, _output_ql, lutcbarpath, _ctnumb, _Sampler, _Projection,
    _CoordinateSystem, SurveyTemplate,  dummy, CatalogFileKey,
    CatalogRadius, CatalogFilter, CatalogFields, sigma, field, CatalogIDs, GridLabels, rgboffset, rgbscale,
    Annotations, DrawFile,  imagej, SIAImageTimeout, PixelOffset, DescriptionXSLT, SurveysTrailer,
    SurveysHeader, _surveyCount, name_, _imageMax, _imageMin, RGBTemplate, _output_rgb,
    small, big, SIABase, SurveyCoordinateSystem, ImageSize, MaxRequestSize, LargeImage, ImageFactory,
    UrlCoordinates,  DSS2Prefix, pos, NAXIS, cframe, Interpolation, format, proj, tileX, tileY, FindRetry,
    MinEdge, MaxRad, ComboSamplers, outputRoot, compressed, RGBWriter, HTMLWriter,
    catalogFile, NOEXIT, finalpostprocessor,  SIAPMaxImages, NullImageDir, ImageFinder,
    Url_VizierBase, Url_HeasarcBase, name,
    _meta_regime, _meta_nsurvey, _meta_frequency, _meta_bandpass, _meta_coverage, _meta_pixelscale,
    _meta_pixelunits, _meta_resolution, _meta_coordinatesystem, _meta_projection, _meta_epoch,
    _meta_reference,

    CATLOG; //TYPO?


    private static Map<String,Key> valuesLowerCaseCache = new HashMap<String, Key>();
    static{
        for(Key k:values()){
            String s = k.name().toLowerCase();
            Key prevKey = valuesLowerCaseCache.put(s,k);
            if(prevKey!=null)
                throw new InternalError("Duplicity in Skyview Key: "+k);
        }
    }

    public static Key valueOfIgnoreCase(String key) {
        Key ret =  valuesLowerCaseCache.get(key.toLowerCase());
        if(ret == null)
            throw new IllegalArgumentException("No enum constant for string: "+key);
        return ret;
    }
}

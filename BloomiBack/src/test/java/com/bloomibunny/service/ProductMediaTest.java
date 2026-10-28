package com.bloomibunny.service;

import com.bloomibunny.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ProductMediaTest {
 @Test void missingFieldPreservesLegacyClientBehavior(){assertNull(ProductMedia.normalize(null,"/products/cover.png"));}
 @Test void galleryIsOrderedUniqueAndNeverRepeatsCover(){assertEquals(List.of("/products/gallery/b.webp","/products/gallery/a.avif"),ProductMedia.normalize(List.of("/products/cover.png"," /products/gallery/b.webp ","/products/gallery/b.webp","/products/gallery/a.avif"),"/products/cover.png"));}
 @Test void emptyGalleryCanBeExplicitlySaved(){assertEquals(List.of(),ProductMedia.normalize(List.of(),null));}
 @Test void unsafeAndLocalFilesystemUrlsAreRejected(){for(var url:List.of("javascript:alert(1)","file:///C:/photos/a.png","//example.com/a.png","/products/../../secret","/products/%2e%2e/secret","/products/with space.png","data:image/png;base64,aaa","https://user:password@example.com/a.png"))assertThrows(BusinessException.class,()->ProductMedia.normalize(List.of(url),null),url);}
 @Test void remoteUrlsAndModernImageFormatsAreAccepted(){assertEquals(List.of("https://example.com/a.webp","http://localhost:5173/products/a.avif"),ProductMedia.normalize(List.of("https://example.com/a.webp","http://localhost:5173/products/a.avif"),null));}
 @Test void excessiveOrBlankEntriesAreRejected(){assertThrows(BusinessException.class,()->ProductMedia.normalize(java.util.Collections.nCopies(21,"/products/a.png"),null));assertThrows(BusinessException.class,()->ProductMedia.normalize(List.of(" "),null));}
}

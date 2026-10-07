package com.qiuqiu.pet;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Read-only content provider for the downloaded installer package. */
public final class UpdateFileProvider extends ContentProvider {
 @Override public boolean onCreate(){return true;}
 private File resolve(Uri uri)throws FileNotFoundException{
  if(getContext()==null||uri.getPathSegments().size()!=1)throw new FileNotFoundException();
  File root=getContext().getExternalFilesDir(null),file=new File(root,uri.getLastPathSegment());
  try{if(root==null||!file.getCanonicalPath().startsWith(root.getCanonicalPath()+File.separator)||!file.isFile())throw new FileNotFoundException();}catch(java.io.IOException e){throw new FileNotFoundException();}
  return file;
 }
 @Override public String getType(Uri uri){String name=uri.getLastPathSegment();String type=name==null?null:java.net.URLConnection.guessContentTypeFromName(name);return type==null?"application/octet-stream":type;}
 @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){try{File f=resolve(uri);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(cols[i])?f.getName():OpenableColumns.SIZE.equals(cols[i])?f.length():null;c.addRow(row);return c;}catch(Exception e){return null;}}
 @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
 @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
 @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
}

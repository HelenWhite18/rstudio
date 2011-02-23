/*
 * RGraphicsPlot.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "RGraphicsPlot.hpp"

#include <iostream>

#include <boost/format.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>

#include <core/system/System.hpp>

#include <r/RExec.hpp>
#include <r/session/RGraphics.hpp>

using namespace core ;

namespace r {
namespace session {
namespace graphics {
      
Plot::Plot(const GraphicsDeviceFunctions& graphicsDevice,
           const FilePath& baseDirPath,
           SEXP manipulatorSEXP)
   : graphicsDevice_(graphicsDevice), 
     baseDirPath_(baseDirPath),
     needsUpdate_(false),
     manipulator_(manipulatorSEXP)
{
}

Plot::Plot(const GraphicsDeviceFunctions& graphicsDevice,
           const FilePath& baseDirPath, 
           const std::string& storageUuid,
           const DisplaySize& renderedSize)
   : graphicsDevice_(graphicsDevice), 
     baseDirPath_(baseDirPath), 
     storageUuid_(storageUuid),
     renderedSize_(renderedSize),
     needsUpdate_(false),
     manipulator_()
{
   // invalidate if the image file doesn't exist (allows the server
   // to migrate between different image backends e.g. png, jpeg, svg)
   if (!imageFilePath(storageUuid_).exists())
      invalidate();
} 
   
std::string Plot::storageUuid() const
{  
   return storageUuid_;
}

void Plot::invalidate()
{
   needsUpdate_ = true;
}

bool Plot::hasManipulator() const
{
   // check is a bit complicated because defer loading the manipulator
   // until it is asked for

   return !manipulator_.empty() || hasManipulatorFile();
}

SEXP Plot::manipulatorSEXP() const
{
   if (hasManipulator())
   {
      loadManipulatorIfNecessary();
      return manipulator_.sexp();
   }
   else
   {
      return R_NilValue;
   }

   return manipulator_.sexp();
}
   
void Plot::manipulatorAsJson(json::Value* pValue) const
{
   if (hasManipulator())
   {
      loadManipulatorIfNecessary();
      manipulator_.asJson(pValue);
   }
   else
   {
      *pValue = json::Value();
   }
}

void Plot::saveManipulator() const
{
   if (hasManipulator() && !storageUuid_.empty())
      saveManipulator(storageUuid_);
}
   
Error Plot::renderFromDisplay()
{
   // we can use our cached representation if we don't need an update and our 
   // rendered size is the same as the current graphics device size
   if ( !needsUpdate_ &&
        (renderedSize() == graphicsDevice_.displaySize()) )
   {
      return Success();
   }
    
   // generate a new storage uuid
   std::string storageUuid = core::system::generateUuid();
   
   // generate snapshot file 
   Error error = graphicsDevice_.saveSnapshot(snapshotFilePath(storageUuid));
   if (error)
      return Error(errc::PlotRenderingError, error, ERROR_LOCATION);
   
   // generate image file
   error = graphicsDevice_.saveAsImageFile(imageFilePath(storageUuid));   
   if (error)
      return Error(errc::PlotRenderingError, error, ERROR_LOCATION);

   // save rendered size
   renderedSize_ = graphicsDevice_.displaySize();
   
   // save manipulator (if any)
   saveManipulator(storageUuid);

   // delete existing files (if any)
   Error removeError = removeFiles();
        
   // update state
   storageUuid_ = storageUuid;
   needsUpdate_ = false;
   
   // return error status 
   return removeError;
}
   
Error Plot::renderFromDisplaySnapshot(SEXP snapshot)
{
   // generate a new storage uuid
   std::string storageUuid = core::system::generateUuid();
 
   // generate snapshot file
   FilePath snapshotFile = snapshotFilePath(storageUuid);
   Error error = r::exec::RFunction(".rs.saveGraphicsSnapshot",
                                    snapshot,
                                    snapshotFile.absolutePath()).call();
   if (error)
      return error ;

   //
   // we can't generate an image file by calling graphicsDevice_.saveAsImageFile
   // because the GraphicsDevice has already moved on to the next page. this is
   // OK though because we simply set needsUpdate_ = true below and the next
   // time renderFromDisplay is called it will be rendered
   //
   
   // save rendered size
   renderedSize_ = graphicsDevice_.displaySize();

   // save manipulator (if any)
   saveManipulator(storageUuid);

   // delete existing files (if any)
   Error removeError = removeFiles();
   
   // update state
   storageUuid_ = storageUuid;
   needsUpdate_ = true;
   
   // return error status
   return removeError;
}
   

std::string Plot::imageFilename() const
{
   return imageFilePath(storageUuid()).filename();
}

Error Plot::renderToDisplay()
{
   Error error = graphicsDevice_.restoreSnapshot(snapshotFilePath());
   if (error)
   {
      Error graphicsError(errc::PlotRenderingError, error, ERROR_LOCATION);
      DisplaySize deviceSize = graphicsDevice_.displaySize();
      graphicsError.addProperty("device-width", deviceSize.width);
      graphicsError.addProperty("device-height", deviceSize.height);
      return graphicsError;
   }
   else
      return Success();
}
   
Error Plot::removeFiles()
{
   // bail if we don't have any storage
   if (storageUuid_.empty())
      return Success();
   
   Error snapshotError = snapshotFilePath(storageUuid_).removeIfExists();
   Error imageError = imageFilePath(storageUuid_).removeIfExists();
   Error manipulatorError = manipulatorFilePath(storageUuid_).removeIfExists();
   
   if (snapshotError)
      return Error(errc::PlotFileError, snapshotError, ERROR_LOCATION);
   else if (imageError)
      return Error(errc::PlotFileError, imageError, ERROR_LOCATION);
   else if (manipulatorError)
      return Error(errc::PlotFileError, manipulatorError, ERROR_LOCATION);
   else
      return Success();
}

void Plot::purgeInMemoryResources()
{
   manipulator_.clear();
}

bool Plot::hasStorage() const
{
   return !storageUuid_.empty();
}

FilePath Plot::snapshotFilePath() const
{
   return snapshotFilePath(storageUuid());
}


FilePath Plot::snapshotFilePath(const std::string& storageUuid) const
{
   return baseDirPath_.complete(storageUuid + ".snapshot");
}
   
FilePath Plot::imageFilePath(const std::string& storageUuid) const
{
   std::string extension = graphicsDevice_.imageFileExtension();
   return baseDirPath_.complete(storageUuid + "." + extension);
}

bool Plot::hasManipulatorFile() const
{
   return hasStorage() && manipulatorFilePath(storageUuid()).exists();
}

FilePath Plot::manipulatorFilePath(const std::string& storageUuid) const
{
   return baseDirPath_.complete(storageUuid + ".manipulator");
}

void Plot::loadManipulatorIfNecessary() const
{   
   if (manipulator_.empty() && hasManipulatorFile())
   {
      FilePath manipPath = manipulatorFilePath(storageUuid());
      Error error = manipulator_.load(manipPath);
      if (error)
         LOG_ERROR(error);
   }
}

void Plot::saveManipulator(const std::string& storageUuid) const
{
   loadManipulatorIfNecessary();
   if (!manipulator_.empty())
   {
      Error error = manipulator_.save(manipulatorFilePath(storageUuid));
      if (error)
         LOG_ERROR(error);
   }
}


} // namespace graphics
} // namespace session
} // namespace r




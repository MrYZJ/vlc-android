/*****************************************************************************
 * TracksProvider.kt
 *****************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.providers.medialibrary

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.videolan.medialibrary.interfaces.AbstractMedialibrary
import org.videolan.medialibrary.interfaces.media.*
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.vlc.util.Settings
import org.videolan.vlc.viewmodels.SortableModel


@ExperimentalCoroutinesApi
class TracksProvider(val parent : MediaLibraryItem?, context: Context, model: SortableModel) : MedialibraryProvider<MediaWrapper>(context, model) {

    override val sortKey = "${super.sortKey}_${parent?.javaClass?.simpleName}"
    override fun canSortByDuration() = true
    override fun canSortByAlbum() = parent !== null
    override fun canSortByLastModified() = true
    override fun canSortByReleaseDate() = true

    override fun isByDisc(): Boolean {
        return parent is AbstractAlbum
    }

    init {
        sort = Settings.getInstance(context).getInt(sortKey, AbstractMedialibrary.SORT_DEFAULT)
        desc = Settings.getInstance(context).getBoolean("${sortKey}_desc", parent is AbstractArtist)
        if (sort == AbstractMedialibrary.SORT_DEFAULT) sort = when (parent) {
            is AbstractArtist -> AbstractMedialibrary.SORT_ALBUM
            is AbstractAlbum -> AbstractMedialibrary.SORT_DEFAULT
            else -> AbstractMedialibrary.SORT_ALPHA
        }
    }

    override fun getAll(): Array<MediaWrapper> = parent?.tracks ?: medialibrary.getAudio(sort, desc)

    override fun getPage(loadSize: Int, startposition: Int) : Array<MediaWrapper> {
        val list = if (model.filterQuery == null) when(parent) {
            is AbstractArtist -> parent.getPagedTracks(sort, desc, loadSize, startposition)
            is AbstractAlbum -> parent.getPagedTracks(sort, desc, loadSize, startposition)
            is AbstractGenre -> parent.getPagedTracks(sort, desc, loadSize, startposition)
            is AbstractPlaylist -> parent.getPagedTracks(loadSize, startposition)
            else -> medialibrary.getPagedAudio(sort, desc, loadSize, startposition)
        } else when(parent) {
            is AbstractArtist -> parent.searchTracks(model.filterQuery, sort, desc, loadSize, startposition)
            is AbstractAlbum -> parent.searchTracks(model.filterQuery, sort, desc, loadSize, startposition)
            is AbstractGenre -> parent.searchTracks(model.filterQuery, sort, desc, loadSize, startposition)
            is AbstractPlaylist -> parent.searchTracks(model.filterQuery, sort, desc, loadSize, startposition)
            else -> medialibrary.searchAudio(model.filterQuery, sort, desc, loadSize, startposition)
        }
        return list.also { completeHeaders(it, startposition) }
    }

    override fun getTotalCount() = if (model.filterQuery == null) when (parent) {
        is AbstractAlbum -> parent.realTracksCount
        is AbstractPlaylist -> parent.realTracksCount
        is AbstractArtist,
        is AbstractGenre -> parent.tracksCount
        else -> medialibrary.audioCount
    } else when(parent) {
        is AbstractArtist -> parent.searchTracksCount(model.filterQuery)
        is AbstractAlbum -> parent.searchTracksCount(model.filterQuery)
        is AbstractGenre -> parent.searchTracksCount(model.filterQuery)
        is AbstractPlaylist -> parent.searchTracksCount(model.filterQuery)
        else ->medialibrary.getAudioCount(model.filterQuery)
    }
}
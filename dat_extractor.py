#!/usr/bin/env python3
"""
Kingdom of the Winds DAT File Extractor
Based on TKViewer format analysis - supports the standard NexusTK DAT archive format
"""

import os
import sys
import struct
import argparse
from pathlib import Path
from typing import List, Dict, Any, Tuple, Optional


class KotWDatExtractor:
    def __init__(self, dat_file_path: str, output_dir: Optional[str] = None):
        self.dat_file = Path(dat_file_path)
        self.output_dir = Path(output_dir) if output_dir else self.dat_file.parent / f"{self.dat_file.stem}_extracted"
        self.file_entries = []
        self.file_info = {}
        
    def analyze_file(self) -> Dict[str, Any]:
        """Analyze the .dat file to determine its format and structure."""
        if not self.dat_file.exists():
            raise FileNotFoundError(f"File not found: {self.dat_file}")
            
        file_size = self.dat_file.stat().st_size
        print(f"Analyzing file: {self.dat_file}")
        print(f"File size: {file_size:,} bytes")
        
        with open(self.dat_file, 'rb') as f:
            # Read first few bytes to identify format
            header = f.read(64)
            format_type = "Unknown DAT Format" # Default
            
            # Try standard DAT format first
            f.seek(0)
            try:
                self._parse_standard_dat_format(f)
                if self.file_entries:
                    format_type = "Standard NexusTK DAT Format"
            except Exception as e:
                print(f"Standard format parsing failed: {e}")
                self.file_entries = [] # Ensure list is empty after failure

            # If standard format fails or finds nothing, try variant
            if not self.file_entries:
                f.seek(0)
                try:
                    self._parse_variant_dat_format(f)
                    if self.file_entries:
                        format_type = "Variant DAT Format (BigSword style)"
                except Exception as e2:
                    print(f"Variant format parsing failed: {e2}")
                    self.file_entries = []

            # If all primary methods fail, attempt a raw scan as a fallback
            if not self.file_entries:
                f.seek(0)
                try:
                    self._parse_raw_directory_scan(f)
                    if self.file_entries:
                        format_type = "Raw Scanned DAT Format"
                except Exception as e3:
                    print(f"Raw directory scan failed: {e3}")
                    self.file_entries = []
        
        self.file_info = {
            'path': str(self.dat_file),
            'size': file_size,
            'header': header,
            'format': format_type,
            'file_count': len(self.file_entries),
            'extractable': len(self.file_entries) > 0
        }
        
        print(f"Header (hex): {header[:16].hex()}")
        print(f"Detected format: {format_type}")
        print(f"Files found: {len(self.file_entries)}")
        
        # Try to find additional files with deep scanning
        if len(self.file_entries) > 0:
            try:
                additional_files = self._scan_for_missing_files()
                if additional_files:
                    print(f"Deep scan found {len(additional_files)} additional files")
                    self.file_entries.extend(additional_files)
                    print(f"Total files after scan: {len(self.file_entries)}")
            except Exception as e:
                print(f"Deep scan failed: {e}")
        
        if self.file_entries:
            print("\nFile listing:")
            for i, (filename, offset, size) in enumerate(self.file_entries[:10]):
                print(f"  {i+1:2d}. {filename:<15} (offset: 0x{offset:08x}, size: {size:,} bytes)")
            if len(self.file_entries) > 10:
                print(f"  ... and {len(self.file_entries) - 10} more files")
        
        return self.file_info
    
    def _parse_standard_dat_format(self, f) -> None:
        """Parse standard NexusTK DAT format as used by TKViewer."""
        # Read file count (plus one, requiring decrement)
        file_count_raw = struct.unpack('<I', f.read(4))[0]
        file_count = file_count_raw - 1  # TKViewer decrements this value
        
        if file_count < 0 or file_count > 10000:  # Sanity check
            raise ValueError(f"Invalid file count: {file_count_raw} -> {file_count}")
        
        print(f"File count: {file_count} (raw: {file_count_raw})")
        
        # Read file allocation table (17-byte entries)
        entries = []
        all_filenames = []  # Track all filenames found
        
        for i in range(file_count):
            entry_start = f.tell()
            
            # Read 4-byte data location offset
            data_offset = struct.unpack('<I', f.read(4))[0]
            
            # Read 13-byte filename (UTF-8, zero-padded)
            filename_bytes = f.read(13)
            # Find null terminator
            null_pos = filename_bytes.find(b'\x00')
            if null_pos != -1:
                filename = filename_bytes[:null_pos].decode('utf-8', errors='ignore')
            else:
                filename = filename_bytes.decode('utf-8', errors='ignore').rstrip('\x00')
            
            all_filenames.append(filename if filename else f"<empty_{i}>")
            
            if filename and data_offset > 0:
                entries.append((filename, data_offset, entry_start))
            elif filename:
                print(f"Found filename but no valid offset: '{filename}' (offset: {data_offset})")
        
        print(f"All filenames found: {all_filenames[:20]}")  # Show first 20
        if len(all_filenames) > 20:
            print(f"... and {len(all_filenames) - 20} more")
        
        # --- Start of new validation logic ---

        # Get total file size for validation
        total_file_size = self.dat_file.stat().st_size

        # 1. Validate filenames for non-printable characters
        for filename, _, _ in entries:
            if not all(c.isprintable() or c in ('\n', '\r', '\t') for c in filename):
                raise ValueError(f"Invalid characters found in filename '{filename}'")

        # 2. Validate offsets
        last_offset = 0
        for filename, offset, _ in entries:
            if offset >= total_file_size:
                raise ValueError(f"Offset 0x{offset:08x} for file '{filename}' is out of bounds (file size: 0x{total_file_size:08x})")
            if offset < last_offset:
                # Allow for unsorted entries, but log it as a potential issue
                # and prepare for sorting later.
                print(f"Warning: Offsets are not strictly increasing. Last: 0x{last_offset:08x}, current: 0x{offset:08x} for '{filename}'")
            last_offset = offset

        # Sort entries by offset to correctly calculate sizes
        entries.sort(key=lambda x: x[1])

        # --- End of new validation logic ---

        # Calculate file sizes based on offset differences
        self.file_entries = []
        for i, (filename, offset, entry_pos) in enumerate(entries):
            if i < len(entries) - 1:
                # Size is difference to next file's offset
                next_offset = entries[i + 1][1]
                file_size = next_offset - offset
            else:
                # Last file extends to end of archive
                file_size = total_file_size - offset
            
            # Additional validation for calculated size
            if file_size < 0:
                 raise ValueError(f"Negative file size calculated for '{filename}' (size: {file_size})")

            if file_size > 0:
                self.file_entries.append((filename, offset, file_size))
    
    def _parse_variant_dat_format(self, f) -> None:
        """Parse variant DAT format (BigSword style with custom headers)."""
        # Read potential format identifier
        format_id = struct.unpack('<I', f.read(4))[0]
        entry_size_or_count = struct.unpack('<I', f.read(4))[0]
        
        # Try to read identifier string
        identifier = f.read(8).decode('ascii', errors='ignore').rstrip('\x00')
        
        print(f"Format ID: {format_id}")
        print(f"Entry size/count: {entry_size_or_count}")
        print(f"Identifier: '{identifier}'")
        
        if identifier in ['BigSword', 'Sword', 'Char', 'Mon'] and entry_size_or_count > 16:
            # Variant format with fixed-size entries
            self._parse_fixed_size_entries(f, entry_size_or_count)
        else:
            # If not a recognized variant, raise an error to allow fallback
            raise ValueError(f"Not a recognized variant DAT format (identifier: '{identifier}')")
    
    def _parse_fixed_size_entries(self, f, entry_size: int) -> None:
        """Parse fixed-size directory entries (like BigSword format)."""
        print(f"Parsing fixed-size entries ({entry_size} bytes each)")
        
        current_pos = 16  # Start after header
        entries_found = []
        all_filenames = []  # Track all filenames found
        
        while current_pos < self.dat_file.stat().st_size:
            f.seek(current_pos)
            entry_data = f.read(entry_size)
            
            if len(entry_data) < entry_size:
                break
            
            # Extract filename (look for null-terminated string at start)
            null_pos = entry_data.find(b'\x00')
            if null_pos == -1 or null_pos == 0:
                break
                
            filename = entry_data[:null_pos].decode('utf-8', errors='ignore')
            all_filenames.append(filename)
            
            # Basic filename validation
            if (len(filename) < 1 or len(filename) > 50 or 
                not all(c.isprintable() or c.isspace() for c in filename)):
                current_pos += entry_size
                continue
            
            # Look for file size and offset in the remaining entry data
            offset, file_size = self._find_offset_and_size(entry_data[null_pos+1:])
            
            if offset > 0 and file_size > 0:
                entries_found.append((filename, offset, file_size))
                print(f"Found: {filename} (offset: 0x{offset:08x}, size: {file_size:,})")
            else:
                # If we can't find offset/size, still report the filename
                if '.' in filename:  # Has an extension, likely a real filename
                    print(f"Found filename but couldn't determine size/offset: {filename}")
            
            current_pos += entry_size
        
        print(f"\nAll filenames discovered: {all_filenames[:30]}")  # Show first 30
        if len(all_filenames) > 30:
            print(f"... and {len(all_filenames) - 30} more")
        
        self.file_entries = entries_found
    
    def _find_offset_and_size(self, data: bytes) -> Tuple[int, int]:
        """Find file offset and size from entry data."""
        # Look for reasonable values in the data
        potential_values = []
        
        for i in range(0, len(data) - 4, 4):
            try:
                value = struct.unpack('<I', data[i:i+4])[0]
                if value > 0:
                    potential_values.append(value)
            except:
                continue
        
        if len(potential_values) < 2:
            return 0, 0
        
        # Find best offset/size combination
        file_size_limit = self.dat_file.stat().st_size
        
        for offset in potential_values:
            if offset > 100 and offset < file_size_limit:  # Reasonable offset
                for size in potential_values:
                    if size != offset and 100 < size < 50000000:  # Reasonable size
                        if offset + size <= file_size_limit:
                            return offset, size
        
        return 0, 0
    
    def _scan_for_missing_files(self) -> List[Tuple[str, int, int]]:
        """Scan the entire file for any missed files by looking for filename patterns."""
        print("\nScanning entire file for additional files...")
        
        with open(self.dat_file, 'rb') as f:
            data = f.read()
        
        additional_files = []
        known_offsets = {offset for _, offset, _ in self.file_entries}
        
        # Common file extensions in Kingdom of the Winds
        extensions = ['.epf', '.pal', '.dna', '.dsc', '.frm', '.tbl', '.wav', '.mid', '.txt', '.cfg']
        
        for ext in extensions:
            search_bytes = ext.encode('utf-8')
            pos = 0
            
            while True:
                pos = data.find(search_bytes, pos)
                if pos == -1:
                    break
                
                # Work backwards to find potential filename start
                filename_start = pos
                while filename_start > 0 and data[filename_start - 1] not in b'\x00\xff':
                    filename_start -= 1
                    if pos - filename_start > 50:  # Reasonable filename length limit
                        break
                
                if filename_start < pos:
                    potential_filename = data[filename_start:pos + len(ext)].decode('utf-8', errors='ignore')
                    
                    # Validate filename
                    if (all(c.isprintable() for c in potential_filename) and 
                        '.' in potential_filename and
                        len(potential_filename) > 3):
                        
                        # Try to find file size after the filename
                        size_search_start = pos + len(ext)
                        
                        # Skip some bytes and look for a reasonable size value
                        for offset_pos in range(size_search_start + 1, min(size_search_start + 50, len(data) - 8), 4):
                            if offset_pos + 8 < len(data):
                                try:
                                    potential_size = struct.unpack('<I', data[offset_pos:offset_pos + 4])[0]
                                    potential_offset = struct.unpack('<I', data[offset_pos + 4:offset_pos + 8])[0]
                                    
                                    # Check if this looks like valid file info
                                    if (100 < potential_size < 50000000 and 
                                        1000 < potential_offset < len(data) and
                                        potential_offset not in known_offsets and
                                        potential_offset + potential_size <= len(data)):
                                        
                                        additional_files.append((potential_filename, potential_offset, potential_size))
                                        known_offsets.add(potential_offset)
                                        print(f"Scan found: {potential_filename} (offset: 0x{potential_offset:08x}, size: {potential_size:,})")
                                        break
                                except:
                                    continue
                
                pos += 1
        
        return additional_files
    
    def _parse_raw_directory_scan(self, f) -> None:
        """
        Parses a format where the directory contains (filename, end_offset) pairs,
        and files are stored contiguously.
        """
        print("Attempting two-pass raw directory scan (end-offset format)...")
        
        # === PASS 1: Find filename and end-offset pairs ===
        f.seek(0)
        data = f.read(min(50000, self.dat_file.stat().st_size))
        
        directory_entries = []
        last_entry_pos = 0
        last_end_offset = 0 # Track the last valid end_offset found
        pos = 8

        while pos < len(data) - 20:
            if data[pos] != 0 and chr(data[pos]).isprintable():
                filename_start = pos
                filename_end = data.find(b'\x00', filename_start)
                
                if filename_end != -1 and (filename_end - filename_start) < 100:
                    potential_filename = data[filename_start:filename_end].decode('utf-8', errors='ignore')
                    
                    if (len(potential_filename) > 3 and '.' in potential_filename and
                        any(potential_filename.lower().endswith(ext) for ext in ['.png', '.xml', '.jpg', '.gif', '.wav', '.mid', '.epf', '.pal', '.dna'])):
                        
                        # --- Best-Fit End-Offset Logic ---
                        scan_pos = filename_end + 1
                        while scan_pos < len(data) and data[scan_pos] == 0:
                            scan_pos += 1
                        
                        candidates = []
                        # Scan a window for all plausible end-offsets
                        for i in range(scan_pos, min(scan_pos + 24, len(data) - 4), 4):
                            try:
                                end_offset = struct.unpack('<I', data[i:i+4])[0]
                                if end_offset > last_end_offset and end_offset < self.dat_file.stat().st_size:
                                    candidates.append((end_offset, i + 4)) # Store position too
                            except:
                                continue

                        # Choose the best candidate (the smallest one greater than the last)
                        if candidates:
                            best_candidate = min(candidates, key=lambda x: x[0])
                            end_offset, entry_pos = best_candidate

                            directory_entries.append({'name': potential_filename, 'end_offset': end_offset})
                            last_end_offset = end_offset
                            last_entry_pos = entry_pos
                            pos = entry_pos
                        else:
                            pos += 1 # Move to next byte if no candidate found
            pos += 1

        if not directory_entries:
            print("Pass 1 did not find any valid directory entries.")
            return

        # === PASS 2: Calculate start offsets and sizes ===
        print(f"\nPass 2: Calculating offsets and sizes for {len(directory_entries)} files...")

        start_offset = last_entry_pos
        final_entries = []
        
        for entry in directory_entries:
            end_offset = entry['end_offset']
            size = end_offset - start_offset

            if size < 0:
                print(f"Warning: Negative size calculated for {entry['name']}. Skipping remaining files.")
                break

            final_entries.append((entry['name'], start_offset, size))
            print(f"  Calculated: {entry['name']:<30} (offset: 0x{start_offset:08x}, size: {size:,})")
            start_offset = end_offset # Next file starts where this one ends

            if start_offset > self.dat_file.stat().st_size:
                print("Warning: Calculated start offset exceeds file size. Aborting.")
                break

        self.file_entries = final_entries
    
    def extract(self, single_file_to_extract: Optional[str] = None) -> bool:
        """Extract files from the DAT archive."""
        if not self.file_entries:
            print("No files found to extract. Run analyze_file() first.")
            return False
        
        # Create output directory
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        files_to_process = self.file_entries
        if single_file_to_extract:
            print(f"\nAttempting to extract single file: {single_file_to_extract}")
            files_to_process = [f for f in self.file_entries if f[0] == single_file_to_extract]
            if not files_to_process:
                print(f"Error: File '{single_file_to_extract}' not found in archive.")
                return False
        else:
            print(f"\nExtracting to: {self.output_dir}")

        success_count = 0
        
        with open(self.dat_file, 'rb') as f:
            for filename, offset, size in files_to_process:
                try:
                    f.seek(offset)
                    file_data = f.read(size)
                    
                    if len(file_data) != size:
                        print(f"Warning: {filename} - expected {size} bytes, got {len(file_data)}")
                        continue
                    
                    output_path = self.output_dir / filename
                    output_path.parent.mkdir(parents=True, exist_ok=True)
                    
                    with open(output_path, 'wb') as out_file:
                        out_file.write(file_data)
                    
                    print(f"✓ Extracted: {filename} ({size:,} bytes)")
                    success_count += 1
                    
                except Exception as e:
                    print(f"✗ Failed to extract {filename}: {e}")
        
        total_files = len(files_to_process)
        print(f"\nExtraction complete: {success_count}/{total_files} files extracted successfully")
        return success_count > 0
    
    def hex_dump(self, num_bytes: int = 256) -> None:
        """Display a hex dump of the file header."""
        print(f"\nHex dump of first {num_bytes} bytes:")
        print("-" * 70)
        
        with open(self.dat_file, 'rb') as f:
            data = f.read(num_bytes)
            
        for i in range(0, len(data), 16):
            chunk = data[i:i+16]
            hex_part = ' '.join(f'{b:02x}' for b in chunk)
            ascii_part = ''.join(chr(b) if 32 <= b <= 126 else '.' for b in chunk)
            print(f'{i:08x}: {hex_part:<48} |{ascii_part}|')
    
    def list_files(self) -> None:
        """List all files in the archive with details."""
        if not self.file_entries:
            print("No files found. Run analyze_file() first.")
            return
        
        print(f"\nFiles in {self.dat_file.name}:")
        print("-" * 70)
        print(f"{'#':<3} {'Filename':<20} {'Offset':<12} {'Size':<12} {'Type':<8}")
        print("-" * 70)
        
        total_size = 0
        for i, (filename, offset, size) in enumerate(self.file_entries, 1):
            file_ext = Path(filename).suffix.upper()[1:] if '.' in filename else 'UNK'
            print(f"{i:<3} {filename:<20} 0x{offset:08x}   {size:>8,}   {file_ext:<8}")
            total_size += size
        
        print("-" * 70)
        print(f"Total: {len(self.file_entries)} files, {total_size:,} bytes")


def extract_single_file(dat_file_path: str, output_dir: str = None, analyze_only: bool = False, 
                       list_files: bool = False, hex_dump_size: int = 0,
                       file_to_extract: Optional[str] = None) -> bool:
    """Extract a single DAT file."""
    try:
        extractor = KotWDatExtractor(dat_file_path, output_dir)
        
        # Analyze the file
        file_info = extractor.analyze_file()
        
        # Show hex dump if requested
        if hex_dump_size > 0:
            extractor.hex_dump(hex_dump_size)
        
        # List files if requested
        if list_files:
            extractor.list_files()
            return True # --list implies no extraction
        
        if analyze_only:
            print("\nAnalysis complete.\n")
            return True
        
        # Attempt extraction
        if file_info['extractable']:
            success = extractor.extract(file_to_extract)
            
            if success:
                print(f"✅ Extraction completed successfully!")
                print(f"Files extracted to: {extractor.output_dir}\n")
                return True
            else:
                print(f"❌ Extraction failed.\n")
                return False
        else:
            print(f"❌ No extractable files found in the archive.\n")
            return False
            
    except Exception as e:
        print(f"Error processing {dat_file_path}: {e}\n")
        return False


def batch_extract_folder(folder_path: str, output_base_dir: str = None, analyze_only: bool = False,
                        list_files: bool = False) -> None:
    """Extract all .dat files in a folder."""
    folder = Path(folder_path)
    
    if not folder.exists() or not folder.is_dir():
        print(f"Error: '{folder_path}' is not a valid directory.")
        return
    
    # Find all .dat files
    dat_files = list(folder.glob("*.dat"))
    dat_files.extend(folder.glob("*.DAT"))  # Case insensitive
    
    if not dat_files:
        print(f"No .dat files found in '{folder_path}'")
        return
    
    print(f"Found {len(dat_files)} .dat files in '{folder_path}':")
    for dat_file in dat_files:
        print(f"  - {dat_file.name}")
    print()
    
    # Process each file
    success_count = 0
    total_files = len(dat_files)
    
    for i, dat_file in enumerate(dat_files, 1):
        print(f"[{i}/{total_files}] Processing {dat_file.name}...")
        print("=" * 60)
        
        # Determine output directory for this file
        if output_base_dir:
            file_output_dir = Path(output_base_dir) / f"{dat_file.stem}_extracted"
        else:
            file_output_dir = None
        
        success = extract_single_file(
            str(dat_file), 
            str(file_output_dir) if file_output_dir else None,
            analyze_only, 
            list_files,
            0  # hex_dump_size
        )
        
        if success:
            success_count += 1
    
    # Summary
    print("=" * 60)
    print(f"Batch processing complete: {success_count}/{total_files} files processed successfully")
    
    if not analyze_only and success_count > 0:
        if output_base_dir:
            print(f"All extracted files are in: {output_base_dir}")
        else:
            print(f"Extracted files are in individual folders within: {folder_path}")


def main():
    parser = argparse.ArgumentParser(description='Extract Kingdom of the Winds .dat files')
    
    # Create mutually exclusive group for single file vs batch mode
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('dat_file', nargs='?', help='Path to a single .dat file to extract')
    group.add_argument('-b', '--batch', help='Batch extract all .dat files in a folder')
    
    parser.add_argument('-o', '--output', help='Output directory (default: <filename>_extracted)')
    parser.add_argument('-a', '--analyze', action='store_true', help='Only analyze files, don\'t extract')
    parser.add_argument('-l', '--list', action='store_true', help='List files in archives')
    parser.add_argument('-x', '--hex', type=int, default=0, help='Show hex dump of first N bytes')
    parser.add_argument('-e', '--extract-one', help='Extract only a single file by its name')
    
    args = parser.parse_args()
    
    try:
        if args.batch:
            # Batch mode - extract all .dat files in folder
            batch_extract_folder(args.batch, args.output, args.analyze, args.list)
        elif args.dat_file:
            # Single file mode
            success = extract_single_file(
                args.dat_file, args.output, args.analyze,
                args.list, args.hex, args.extract_one
            )
            if not success:
                sys.exit(1)
        else:
            parser.print_help()
            sys.exit(1)
            
    except KeyboardInterrupt:
        print("\nOperation cancelled by user.")
        sys.exit(1)
    except Exception as e:
        print(f"Unexpected error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
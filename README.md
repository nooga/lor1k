# lor1k

lor1k is an attempt at re-implementing Sebastian Macke's jor1k (https://github.com/s-macke/jor1k) in Java.
The goal of the project is to have a minimal, network enabled linux system running completely within JVM.
The secondary goal of this project is to have fun :)

### Status

- It's an early stage, the code is obnoxious and full of lame comments :)
- Loads the kernel image from ELF file
- Early kernel messages appear on the output
- Kernel crashes during uart device driver init

### Building & Running

This repository contains IntelliJ Idea project and contains a pre-built linux kernel image.
1. Clone this repository, 
2. Open the project in IntelliJ Idea,
3. Run the `Main` class.

### Building linux for lor1k

1. Follow the instructions here https://openrisc.io/newlib/ to obtain or1k-elf toolchain,
2. Follow the instructions here https://github.com/kdgwill/OR1K/wiki/Build-Linux-Kernel-For-OpenRisc-1000 to build the kernel.
3. Put the `vmlinux` elf file in `kernel/` and it lor1k will attempt to boot it.

### License 

This project is licensed under BSD 2-clause license, same as jor1k. See `LICENSE` file for more information.

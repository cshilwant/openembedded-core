require systemd.inc
FILESEXTRAPATHS =. "${FILE_DIRNAME}/systemd:"

require conf/image-uefi.conf

DEPENDS = "intltool-native libcap util-linux gperf-native python3-jinja2-native python3-pyelftools-native"

inherit meson pkgconfig gettext
inherit deploy

LDFLAGS:prepend = "${@ " ".join(d.getVar('LD').split()[1:])} "

EFI_LD = "bfd"

do_write_config[vardeps] += "CC OBJCOPY EFI_LD"
do_write_config:append() {
    cat >${WORKDIR}/meson-${PN}.cross <<EOF
[binaries]
c = ${@meson_array('CC', d)}
objcopy = ${@meson_array('OBJCOPY', d)}
c_ld = ${@meson_array('EFI_LD', d)}
EOF
}

EXTRA_OEMESON += "-Defi=true \
                  -Dbootloader=true \
                  -Dman=false \
                  --cross-file ${WORKDIR}/meson-${PN}.cross \
                  "

# install to the image as boot*.efi if its the EFI_PROVIDER,
# otherwise install as the full name.
# This allows multiple bootloaders to coexist in a single image.
python __anonymous () {
    import re
    target = d.getVar('TARGET_ARCH')
    prefix = "" if d.getVar('EFI_PROVIDER') == "systemd-boot" else "systemd-"
    systemdimage = prefix + d.getVar("EFI_BOOT_IMAGE")
    d.setVar("SYSTEMD_BOOT_IMAGE", systemdimage)
    prefix = "systemd-" if prefix == "" else ""
    d.setVar("SYSTEMD_BOOT_IMAGE_PREFIX", prefix)
}

FILES:${PN} = "${EFI_FILES_PATH}/${SYSTEMD_BOOT_IMAGE}"

RDEPENDS:${PN} += "virtual-systemd-bootconf"

# Imported from the old gummiboot recipe
TUNE_CCARGS:remove = "-mfpmath=sse"

COMPATIBLE_HOST = "(aarch64.*|arm.*|x86_64.*|i.86.*)-linux"
COMPATIBLE_HOST:x86-x32 = "null"

do_compile() {
	ninja systemd-boot
}

do_install() {
	install -d ${D}${EFI_FILES_PATH}
	install ${B}/src/boot/efi/systemd-boot*.efi ${D}${EFI_FILES_PATH}/${SYSTEMD_BOOT_IMAGE}
}

do_deploy () {
	install ${B}/src/boot/efi/systemd-boot*.efi ${DEPLOYDIR}
	install ${B}/src/boot/efi/linux*.efi.stub ${DEPLOYDIR}
	install ${B}/src/boot/efi/addon*.efi.stub ${DEPLOYDIR}
}

addtask deploy before do_build after do_compile


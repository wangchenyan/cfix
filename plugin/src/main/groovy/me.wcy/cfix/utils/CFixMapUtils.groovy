package me.wcy.cfix.utils

class CFixMapUtils {
    private static final String MAP_SEPARATOR = ":"

    static boolean notSame(Map map, String name, String hash) {
        boolean notSame = false
        if (map) {
            String value = map.get(name)
            if (value) {
                if (!value.equals(hash)) {
                    notSame = true
                }
            } else {
                notSame = true
            }
        }
        return notSame
    }

    static Map parseMap(File hashFile) {
        Map hashMap = [:]
        if (hashFile.exists()) {
            hashFile.eachLine {
                List list = it.split(MAP_SEPARATOR)
                if (list.size() == 2) {
                    hashMap.put(list[0], list[1])
                }
            }
        } else {
            CFixLogger.i("$hashFile does not exist")
        }
        return hashMap
    }

    static format(String name, String hash) {
        return name + MAP_SEPARATOR + hash + "\n"
    }
}

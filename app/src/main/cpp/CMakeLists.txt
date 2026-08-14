cmake_minimum_required(VERSION 3.22.1)
project("mealoo_native")

# 1. 移除所有 標記，那些是筆記不是程式碼
set(CMAKE_CXX_STANDARD 17)
set(LLAMA_DIR ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp)

# 2. 核心：直接把 llama.cpp 當作子專案引入
# 這會自動處理所有 ggml 和核心檔案的編譯，不需要手動列舉
if(EXISTS "${LLAMA_DIR}/CMakeLists.txt")
    add_subdirectory(${LLAMA_DIR} ${CMAKE_CURRENT_BINARY_DIR}/llama_build)
else()
    message(FATAL_ERROR "找不到 llama.cpp！請執行 git clone。路徑：${LLAMA_DIR}")
endif()
set(GGML_VULKAN ON)
# 3. 定義你自己的 JNI 橋接檔案
add_library(mealoo_native SHARED native-lib.cpp)

target_link_libraries(mealoo_native
        llama
        ggml     # 底層張量庫
        log
        android
)

target_include_directories(mealoo_native PRIVATE
        ${LLAMA_DIR}/include
        ${LLAMA_DIR}/ggml/include
)
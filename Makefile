ifneq ($(wildcard .classpath),)
	SOURCE  := $(shell bash -c 'grep "kind=\"src\"" .classpath | sed -r "s/.*<classpathentry kind=\"src\" path=\"(.*)\"\/>/\1/"')
	BUILD   := $(shell bash -c 'grep "kind=\"output\"" .classpath | sed -r "s/.*<classpathentry kind=\"output\" path=\"(.*)\"\/>.*/\1/"')
else
	SOURCE  := src
	BUILD   := bin
endif
	DOC     := doc

PROG = NetDot
SRCS = $(shell find $(SOURCE) -name '*.java')
OBJS = $(SRCS:$(SOURCE)/%.java=$(BUILD)/%.class)
LIBS =

CFLAGS =

all: $(BUILD) $(OBJS)
	@echo 'Compiled to $(BUILD)/'
#	@$(MAKE) $(PROG) --no-print-directory

run: all
	java -cp $(BUILD)/ $(PROG) $(filter-out $@,$(MAKECMDGOALS))

jar: all
	jar --create -f $(PROG).jar -e $(PROG) -C $(BUILD) .

clean:
	rm -f $(OBJS) $(PROG).jar #$(BUILD)/.compile_*
	rm -rf $(DOC)/*
	@/bin/echo -e '\e[1;32mClean...\e[0m'

doc:
	javadoc -sourcepath $(SOURCE)/ -cp $(BUILD)/ -d $(DOC)/ $(SRCS)

#$(PROG): $(OBJS)
#	@echo 'Compiled to $(BUILD)/'

#debug: CFLAGS += -g
#debug: $(BUILD)/.compile_debug $(OBJS)
#	@$(MAKE) $(PROG) --no-print-directory

.PHONY: all run jar clean $(DOC) #$(PROG) debug

$(BUILD)/%.class: $(SOURCE)/%.java
	javac -d $(BUILD)/ -cp $(SOURCE) $<
#	/usr/lib/jvm/java-8-openjdk/bin/javac -d $(BUILD)/ -cp $(SOURCE) $<

$(BUILD):
	mkdir -p $@ # $(@D)

#$(BUILD)/.compile_normal: | $(BUILD)
#ifneq ("$(wildcard $(BUILD)/.compile_debug)", "")
#	@/bin/echo -e "\e[1;34mPreviously compiled with debug flags, recompiling...\e[0m"
#	$(MAKE) clean --no-print-directory
#endif
#	@touch $(BUILD)/.compile_normal
#
#$(BUILD)/.compile_debug: | $(BUILD)
#ifneq ("$(wildcard $(BUILD)/.compile_normal)", "")
#	@/bin/echo -e "\e[1;34mCode wasn't compiled with debug flags, recompiling...\e[0m" $(MAKE) clean --no-print-directory
#endif
#	@touch $(BUILD)/.compile_debug

%:
	@:

package pl.upir.blog.web.app.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.upir.blog.entity.BlgDicCategory;
import pl.upir.blog.entity.BlgDicTag;
import pl.upir.blog.entity.BlgPost;
import pl.upir.blog.entity.BlgUser;
import pl.upir.blog.service.BlgDicCategoryService;
import pl.upir.blog.service.BlgDicTagService;
import pl.upir.blog.service.BlgPostService;
import pl.upir.blog.service.BlgUserService;
import pl.upir.blog.service.security.BlgUserSecurityServiceImpl;
import pl.upir.blog.web.form.Message;
import pl.upir.blog.web.util.ImageCropper;
import pl.upir.blog.web.util.UrlUtil;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Vitalii on 28/08/2015.
 */
@Controller
@RequestMapping("/")
public class BlgPostController {
    final Logger logger = LoggerFactory.getLogger(BlgHomeController.class);

    @Autowired
    private MessageSource messageSource;
    @Autowired
    BlgUserService blgUserService;
    @Autowired
    private BlgUserSecurityServiceImpl blgUserSecurityService;
    @Autowired
    BlgPostService blgPostService;
    @Autowired
    BlgDicCategoryService blgDicCategoryService;
    @Autowired
    BlgDicTagService blgDicTagService;

    /*static class BlgTagCat {
        public List<BlgPostCategory> blgPostCategories;
        public List<BlgDicTag> blgDicTags;


    }*/

    @Secured("IS_AUTHENTICATED_FULLY")
    @RequestMapping(value = "/{firstName}.{lastName}/post/create", method = RequestMethod.GET)
    //@RequestMapping(value = "/post/create", method = RequestMethod.GET)
    public String home(Model model) {

        //Set<BlgDicTag> blgDicTagList = new HashSet<>(blgDicTagService.findAll());
        List<BlgDicTag> blgDicTagList = blgDicTagService.findAll();
        //Set<BlgPostCategory> blgPostCategories = new HashSet<>(blgDicCategoryService.findAll());
        List<BlgDicCategory> blgPostCategoriesList = blgDicCategoryService.findAll();
        BlgPost blgPost = new BlgPost();

        model.addAttribute("blgPostCatList", blgPostCategoriesList);
        model.addAttribute("blgPostTagList", blgDicTagList);
        model.addAttribute("blgPost", blgPost);
        return "post/create";
    }

    @ModelAttribute("tagCache")
    public List<BlgDicTag> getDicTag(){
        return blgDicTagService.findAll();
    }

    /*protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
        binder.registerCustomEditor(Set.class, "blgDicTagSet", new CustomCollectionEditor(Set.class)
        {
            protected Object convertElement(Object element)
            {
                String name = "";
                int id = 0;

                if (element instanceof String) {
                    name = (String) element;
                    BlgDicTag blgDicTag = blgDicTagService.finByDicTagName(name);
                    return name != null ? blgDicTag : null;
                }
                else
                    return null;
            }
        });


    }*/

    @Secured("IS_AUTHENTICATED_FULLY")
    @RequestMapping(value = "/{firstName}.{lastName}/post/create", method = RequestMethod.POST)
    public String create(@Valid @ModelAttribute("blgPost") BlgPost blgPost, BindingResult bindingUser,
                         //@RequestParam(value = "pstTitleImage", required = false)
                         //@ModelAttribute("pstTitleImage")MultipartFile file,
                                   Model model, HttpServletRequest request,
                                   RedirectAttributes redirectAttributes, Locale locale) {

        if (bindingUser.hasErrors()) {
            List<BlgDicTag> blgDicTagList = blgDicTagService.findAll();
            List<BlgDicCategory> blgPostCategoriesList = blgDicCategoryService.findAll();
            model.addAttribute("message", new Message("alert alert-danger", "Oh snap!", messageSource.getMessage("save_fail", new Object[]{}, locale)));
            model.addAttribute("blgPost", blgPost);
            model.addAttribute("blgPostCatList", blgPostCategoriesList);
            model.addAttribute("blgPostTagList", blgDicTagList);

           return "/{firstName}.{lastName}/post/create";
        }
        model.asMap().clear();
        redirectAttributes.addFlashAttribute("message", new Message("alert alert-success", "Well done!", messageSource.getMessage("save_success", new Object[]{}, locale)));
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        BlgUser blgUser = blgUserService.findById(((BlgUser) principal).getUsrId());

        Set<BlgDicTag> blgDicTagSet = blgPost.getBlgDicTagSet();
        blgDicTagSet.forEach(blgDicTag -> blgDicTag.setDicTagId(blgDicTagService.finByDicTagName(blgDicTag.getDicTagName()).getDicTagId()));
        Set<BlgDicCategory> blgDicCategorySet = new HashSet<>();
        blgPost.getBlgDicCategorySet().forEach(blgDicCategory -> {
            blgDicCategory=blgDicCategoryService.findByTitle(blgDicCategory.getDicCatName());
        blgDicCategorySet.add(blgDicCategory);
        });

        int target=50;
        String substr="";
        //Document document = Jsoup.parse(blgPost.getPstDocument());
        String patt="\\s|\\n";
        String documentShort=blgPost.getPstDocument().replaceAll("[>][\\s*]+[<]","><");
        Pattern pattern1=Pattern.compile("\\s|\\n");
        Pattern pattern = Pattern.compile("[.][\\s]?");
        //Pattern pattern2 = Pattern.compile("^[А-Я,A-Z][.*\\s]+$");

        Matcher matcher1=pattern1.matcher(documentShort);
        int i=0;
        while (matcher1.find()){
            if(i==target){
                target=matcher1.end();
            }
            i++;
        }
int j=0;
        matcher1=pattern.matcher(documentShort).region(target,documentShort.length());
        while (matcher1.find()){
            if(matcher1.end()>target) {
                target = matcher1.end();
                j++;
                break;
            }


        }
        //target=matcher1.end();

        pattern1= Pattern.compile("<pre");
        matcher1=pattern1.matcher(documentShort).region(0,target);
        if(matcher1.find()){
            pattern1=Pattern.compile("</pre>");
            matcher1=pattern1.matcher(documentShort).region(target,documentShort.length());
            if(matcher1.find()){
                target=matcher1.end();
            }
        }
        substr=documentShort.substring(0,target);


        /*String str = documentShort.split(patt)[i];
        Matcher matcher = pattern.matcher(str);
        if(matcher.find()){
            int j=i;
            matcher.reset();
            while(matcher.find()){
                j--;
                String str2=documentShort.split("[.*\\s]+")[j];
                matcher=pattern2.matcher(str2);
            }
            for(int q=j;q==i;q++){
                substr=substr+documentShort.split("[.*\\s]+")[q];
            }
        }else
        {
            int j=i;
            matcher.reset();
            while(!matcher.find()){
                j++;
                String str2= documentShort.split(patt)[j];
                matcher=pattern.matcher(str2);
            }

             substr=documentShort.split(patt)[j].replaceAll("\\(\\)","\\\\(\\\\)").replaceAll("[.]", "[.]");
             substr.replaceAll(substr,substr+"<!--START-->");
            documentShort.split(patt)[j]=substr;
             Pattern pattern3 = Pattern.compile("(((?s)"+documentShort.split(patt)[i]+"(.*?)("+documentShort.split(patt)[j]+"))((.*)[.*\\\\s]))\"");
             Matcher matcher1 = pattern3.matcher(documentShort);

            if(matcher1.find()){
                System.out.print("");
            }


        }*/







        blgPost.setPstDocumentShort(substr);
        blgPost.setBlgDicTagSet(blgDicTagSet);
        blgPost.setBlgDicCategorySet(blgDicCategorySet);
        blgPost.getBlgUserSet().add(blgUser);
        MultipartFile file = blgPost.file;


        if (!file.isEmpty()) {
            String fileName = file.getOriginalFilename();
            byte[] bytes = new byte[0];
            try {
                bytes = file.getBytes();
                String path = request.getRealPath("/");
                File serverFile = new File(path + "public/images/post/" + fileName);
                BufferedOutputStream stream =
                        new BufferedOutputStream(new FileOutputStream(serverFile));
                stream.write(bytes);
                stream.close();
                //ImageCropper.resizeImage(serverFile, origHeight, origWidth, "png");
                if(ImageCropper.getSize(serverFile).get("height")>800 && ImageCropper.getSize(serverFile).get("height")>600){
                    model.addAttribute("message", new Message("alert alert-danger", "Oh snap!", messageSource.getMessage("post.image.size", new Object[]{}, locale)));
                    model.addAttribute("blgPost", blgPost);
                    List<BlgDicTag> blgDicTagList = blgDicTagService.findAll();
                    List<BlgDicCategory> blgPostCategoriesList = blgDicCategoryService.findAll();
                    model.addAttribute("blgPostCatList", blgPostCategoriesList);
                    model.addAttribute("blgPostTagList", blgDicTagList);
                    return "/{firstName}.{lastName}/post/create";
                }
                //ImageCropper.cropp(serverFile, "png", height, width, left, top);
                //String oldFilePath = blgUserUpdate.getBlgUserDetail().getUsrPhotoLink();

                /*if (!oldFilePath.equals(UrlUtil.sourcePathFile(request, "/resources/images/" + fileName))) {
                    File oldFile = new File(path + "public/images/" + oldFilePath.substring(oldFilePath.lastIndexOf("/") + 1, oldFilePath.length()));
                    if (oldFile.delete())
                        logger.info("File " + serverFile + " is deleted!");
                    else
                        logger.error("Delete operation is faild!");
                }*/

                blgPost.setPstTitleImage(UrlUtil.sourcePathFile(request, "/resources/images/post/" + fileName));
            } catch (IOException ex){
                ex.printStackTrace();
                //return ex.toString();
            }
        }

        LocalDate date = LocalDate.now();
        blgPost.setPstUrl(UrlUtil.sourcePathFile(request,"/"+date.getYear()+"/"+date.getMonthValue()+"/"+date.getDayOfMonth()+"/"+blgPostService.getNextAutoincrement()));
        //blgPost=blgPostService.save(blgPost);

        blgPostService.save(blgPost);
        return "redirect:/";
    }
}
